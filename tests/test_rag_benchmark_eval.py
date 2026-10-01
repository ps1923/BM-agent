from __future__ import annotations

import copy
import contextlib
import importlib.util
import io
import json
import math
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
MODULE_SPEC = importlib.util.spec_from_file_location(
    "evaluate_rag_benchmark", ROOT / "scripts/evaluate_rag_benchmark.py"
)
EVALUATOR = importlib.util.module_from_spec(MODULE_SPEC)
assert MODULE_SPEC.loader is not None
MODULE_SPEC.loader.exec_module(EVALUATOR)


def tiny_inputs():
    benchmark = [
        {"id": "A", "title": "A", "goldKeywords": ["Spring", "启动"]},
        {"id": "B", "title": "B", "goldKeywords": ["Flask"]},
    ]
    results = {
        "model": "test-model",
        "cases": [
            {
                "id": "A", "retrievedIds": ["eval-A", "other-1"], "retrievalHit": True,
                "noRag": {"answer": "spring 启动", "coverage": 1.0, "seconds": 1.0, "error": None},
                "withRag": {"answer": "Spring", "coverage": 0.5, "seconds": 2.0, "error": None},
            },
            {
                "id": "B", "retrievedIds": ["other-2"], "retrievalHit": False,
                "noRag": {"answer": "Flask app", "coverage": 1.0, "seconds": 3.0, "error": None},
                "withRag": {"answer": "unrelated", "coverage": 0.0, "seconds": 4.0, "error": None},
            },
        ],
    }
    return benchmark, results


class RagBenchmarkEvaluatorTests(unittest.TestCase):
    def test_recomputes_repository_results_and_stored_metrics(self):
        benchmark = EVALUATOR.load_json(EVALUATOR.DEFAULT_BENCHMARK)
        results = EVALUATOR.load_json(EVALUATOR.DEFAULT_RESULTS)

        report = EVALUATOR.analyze(benchmark, results)

        self.assertEqual(report["case_count"], 20)
        self.assertEqual(report["retrieval_hit_at_3"], {"hits": 20, "rate": 1.0})
        self.assertEqual(report["arms"]["noRag"]["mean_per_case_keyword_coverage"], 0.6833)
        self.assertEqual(report["arms"]["withRag"]["mean_per_case_keyword_coverage"], 0.9)
        self.assertEqual(report["arms"]["noRag"]["all_keywords_hit_cases"], 8)
        self.assertEqual(report["arms"]["withRag"]["all_keywords_hit_cases"], 15)
        self.assertEqual(report["arms"]["noRag"]["mean_model_response_seconds"], 1.658)
        self.assertEqual(report["arms"]["withRag"]["mean_model_response_seconds"], 1.597)

    def test_reports_keyword_proxy_and_latency_limitations(self):
        report = EVALUATOR.analyze(*tiny_inputs())
        text = EVALUATOR.render_text(report)

        self.assertIn("模型回答生成平均耗时", text)
        self.assertIn("总体覆盖率 = Σ覆盖率_i / N", text)
        self.assertIn("Hit@3 =", text)
        self.assertIn("不等同于专家判定", text)
        self.assertEqual(report["arms"]["noRag"]["mean_per_case_keyword_coverage"], 1.0)
        self.assertEqual(report["arms"]["withRag"]["mean_per_case_keyword_coverage"], 0.25)

    def test_rejects_duplicate_case_ids(self):
        benchmark, results = tiny_inputs()
        benchmark.append(copy.deepcopy(benchmark[0]))

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "重复样例 ID: A"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_missing_result_case(self):
        benchmark, results = tiny_inputs()
        results["cases"].pop()

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "缺少结果: B"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_empty_keywords(self):
        benchmark, results = tiny_inputs()
        benchmark[0]["goldKeywords"] = []

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "goldKeywords 必须是非空数组"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_empty_answer(self):
        benchmark, results = tiny_inputs()
        results["cases"][0]["noRag"]["answer"] = "  "

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "answer 为空或非字符串"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_extra_result_case(self):
        benchmark, results = tiny_inputs()
        results["cases"].append(copy.deepcopy(results["cases"][0]))
        results["cases"][-1]["id"] = "C"

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "多余结果: C"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_stored_coverage_mismatch(self):
        benchmark, results = tiny_inputs()
        results["cases"][0]["withRag"]["coverage"] = 0.9

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "样例 A 的 withRag.coverage"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_incorrect_retrieval_hit_flag(self):
        benchmark, results = tiny_inputs()
        results["cases"][0]["retrievalHit"] = False

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "retrievalHit 存档值"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_failed_model_call_instead_of_skewing_means(self):
        benchmark, results = tiny_inputs()
        results["cases"][1]["noRag"]["error"] = "timeout"

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "调用记录错误"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_non_string_error_metadata(self):
        benchmark, results = tiny_inputs()
        results["cases"][0]["noRag"]["error"] = False

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "调用记录错误"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_duplicate_json_keys_and_invalid_utf8(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "input.json"
            path.write_text('{"id":"first","id":"second"}', encoding="utf-8")
            with self.assertRaisesRegex(EVALUATOR.EvaluationError, "重复字段: id"):
                EVALUATOR.load_json(path)

            path.write_bytes(b"\xff\xfe")
            with self.assertRaisesRegex(EVALUATOR.EvaluationError, "不是有效 UTF-8"):
                EVALUATOR.load_json(path)

            path.write_text("{invalid JSON", encoding="utf-8")
            with self.assertRaisesRegex(EVALUATOR.EvaluationError, "JSON 格式错误") as caught:
                EVALUATOR.load_json(path)
            self.assertIn(str(path), str(caught.exception))

    def test_rejects_integer_outside_float_range(self):
        benchmark, results = tiny_inputs()
        results["cases"][0]["noRag"]["seconds"] = 10**500

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "无法表示为有限数值"):
            EVALUATOR.analyze(benchmark, results)

    def test_rejects_non_finite_metrics(self):
        benchmark, results = tiny_inputs()
        results["cases"][0]["noRag"]["seconds"] = float("nan")

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "必须是有限"):
            EVALUATOR.analyze(benchmark, results)

    def test_stable_mean_handles_large_finite_response_times(self):
        benchmark, results = tiny_inputs()
        maximum = sys.float_info.max
        for case in results["cases"]:
            case["noRag"]["seconds"] = maximum

        report = EVALUATOR.analyze(benchmark, results)

        mean_seconds = report["arms"]["noRag"]["mean_model_response_seconds"]
        self.assertTrue(math.isfinite(mean_seconds))
        self.assertEqual(mean_seconds, maximum)
        json.dumps(report, allow_nan=False)

    def test_rejects_non_string_title(self):
        benchmark, results = tiny_inputs()
        benchmark[0]["title"] = ["unexpected", "array"]

        with self.assertRaisesRegex(EVALUATOR.EvaluationError, "title 必须是字符串"):
            EVALUATOR.analyze(benchmark, results)

    def test_does_not_mutate_input_objects(self):
        benchmark, results = tiny_inputs()
        originals = json.dumps([benchmark, results], sort_keys=True)

        EVALUATOR.analyze(benchmark, results)

        self.assertEqual(json.dumps([benchmark, results], sort_keys=True), originals)

    def test_cli_returns_nonzero_and_explains_invalid_input(self):
        error_output = io.StringIO()
        with patch.object(EVALUATOR, "load_json", side_effect=EVALUATOR.EvaluationError("样例 X 无效")):
            with contextlib.redirect_stderr(error_output):
                exit_code = EVALUATOR.main(["--benchmark", "missing.json", "--results", "results.json"])

        self.assertEqual(exit_code, 1)
        self.assertIn("评估失败：样例 X 无效", error_output.getvalue())

    def test_cli_data_error_names_input_paths_and_case(self):
        benchmark, results = tiny_inputs()
        benchmark[0]["goldKeywords"] = []
        error_output = io.StringIO()
        with patch.object(EVALUATOR, "load_json", side_effect=[benchmark, results]):
            with contextlib.redirect_stderr(error_output):
                exit_code = EVALUATOR.main([
                    "--benchmark", "benchmark.json", "--results", "results.json"
                ])

        message = error_output.getvalue()
        self.assertEqual(exit_code, 1)
        self.assertIn("benchmark.json", message)
        self.assertIn("results.json", message)
        self.assertIn("样例 A", message)

    def test_cli_missing_input_file_returns_nonzero_with_path(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            missing = Path(temp_dir) / "missing-benchmark.json"
            error_output = io.StringIO()
            with contextlib.redirect_stderr(error_output):
                exit_code = EVALUATOR.main([
                    "--benchmark", str(missing), "--results", "unused-results.json"
                ])

        self.assertEqual(exit_code, 1)
        self.assertIn(str(missing), error_output.getvalue())

    def test_cli_invalid_input_does_not_modify_source_files(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            benchmark_path = Path(temp_dir) / "benchmark.json"
            results_path = Path(temp_dir) / "results.json"
            benchmark_path.write_text("[]", encoding="utf-8")
            results_path.write_text('{"cases":[]}', encoding="utf-8")
            before = (benchmark_path.read_bytes(), results_path.read_bytes())
            with contextlib.redirect_stderr(io.StringIO()):
                exit_code = EVALUATOR.main([
                    "--benchmark", str(benchmark_path), "--results", str(results_path)
                ])

            self.assertEqual(exit_code, 1)
            self.assertEqual(benchmark_path.read_bytes(), before[0])
            self.assertEqual(results_path.read_bytes(), before[1])


if __name__ == "__main__":
    unittest.main()
