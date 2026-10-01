"""Recompute descriptive metrics for the checked-in RAG A/B benchmark."""

from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path
from statistics import fmean
from typing import Any


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BENCHMARK = REPOSITORY_ROOT / "backend/src/test/resources/rag/benchmark-cases.json"
DEFAULT_RESULTS = REPOSITORY_ROOT / "_bmad-output/implementation-artifacts/rag-ab-results-20260919.json"
ARMS = ("noRag", "withRag")


class EvaluationError(ValueError):
    """Raised when an input cannot support a trustworthy report."""


def load_json(path: Path) -> Any:
    try:
        with path.open("r", encoding="utf-8") as source:
            return json.load(source, object_pairs_hook=_object_without_duplicate_keys)
    except EvaluationError as error:
        raise EvaluationError(f"JSON 数据错误 {path}: {error}") from error
    except OSError as error:
        raise EvaluationError(f"无法读取文件 {path}: {error}") from error
    except json.JSONDecodeError as error:
        raise EvaluationError(f"JSON 格式错误 {path}:{error.lineno}:{error.colno}: {error.msg}") from error
    except UnicodeDecodeError as error:
        raise EvaluationError(f"文件不是有效 UTF-8 JSON {path}: {error}") from error


def _object_without_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise EvaluationError(f"JSON 对象包含重复字段: {key}")
        result[key] = value
    return result


def _index_by_id(items: Any, label: str) -> tuple[list[str], dict[str, dict[str, Any]]]:
    if not isinstance(items, list):
        raise EvaluationError(f"{label} 必须是数组")

    order: list[str] = []
    indexed: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            raise EvaluationError(f"{label}[{index}] 必须是对象")
        case_id = item.get("id")
        if not isinstance(case_id, str) or not case_id.strip():
            raise EvaluationError(f"{label}[{index}].id 必须是非空字符串")
        if case_id in indexed:
            raise EvaluationError(f"{label} 中存在重复样例 ID: {case_id}")
        indexed[case_id] = item
        order.append(case_id)
    return order, indexed


def _keywords_for(case: dict[str, Any], case_id: str) -> list[str]:
    keywords = case.get("goldKeywords")
    if not isinstance(keywords, list) or not keywords:
        raise EvaluationError(f"样例 {case_id} 的 goldKeywords 必须是非空数组")
    if any(not isinstance(keyword, str) or not keyword.strip() for keyword in keywords):
        raise EvaluationError(f"样例 {case_id} 的 goldKeywords 含空值或非字符串")
    normalized = [keyword.casefold() for keyword in keywords]
    if len(set(normalized)) != len(normalized):
        raise EvaluationError(f"样例 {case_id} 的 goldKeywords 存在重复项")
    return keywords


def _number(value: Any, label: str, case_id: str, *, minimum: float) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise EvaluationError(f"样例 {case_id} 的 {label} 必须是数值")
    try:
        number = float(value)
    except (OverflowError, ValueError) as error:
        raise EvaluationError(f"样例 {case_id} 的 {label} 无法表示为有限数值") from error
    if not math.isfinite(number) or number < minimum:
        raise EvaluationError(f"样例 {case_id} 的 {label} 必须是有限且不小于 {minimum} 的数值")
    return number


def _stable_mean(values: list[float]) -> float:
    scale = max(values)
    if scale == 0.0:
        return 0.0
    normalized_mean = fmean(value / scale for value in values)
    return scale * min(normalized_mean, 1.0)


def _round_finite(value: float, digits: int) -> float:
    try:
        rounded = round(value, digits)
    except OverflowError:
        return value
    return rounded if math.isfinite(rounded) else value


def _answer_metrics(answer_data: Any, keywords: list[str], case_id: str, arm: str) -> tuple[float, float]:
    if not isinstance(answer_data, dict):
        raise EvaluationError(f"样例 {case_id} 缺少 {arm} 结果对象")
    if answer_data.get("error") not in (None, ""):
        raise EvaluationError(f"样例 {case_id} 的 {arm} 调用记录错误: {answer_data.get('error')}")

    answer = answer_data.get("answer")
    if not isinstance(answer, str) or not answer.strip():
        raise EvaluationError(f"样例 {case_id} 的 {arm}.answer 为空或非字符串")
    answer_folded = answer.casefold()
    coverage = sum(keyword.casefold() in answer_folded for keyword in keywords) / len(keywords)

    reported = _number(answer_data.get("coverage"), f"{arm}.coverage", case_id, minimum=0.0)
    if reported > 1.0:
        raise EvaluationError(f"样例 {case_id} 的 {arm}.coverage 超出 [0, 1]")
    if not math.isclose(reported, coverage, rel_tol=0.0, abs_tol=0.0000501):
        raise EvaluationError(
            f"样例 {case_id} 的 {arm}.coverage 存档值 {reported:.4f} 与重算值 {coverage:.4f} 不符"
        )

    seconds = _number(answer_data.get("seconds"), f"{arm}.seconds", case_id, minimum=0.0)
    return coverage, seconds


def analyze(
    benchmark_data: Any,
    results_data: Any,
    *,
    benchmark_source: str = "benchmark cases",
    results_source: str = "result cases",
) -> dict[str, Any]:
    benchmark_order, benchmark = _index_by_id(benchmark_data, benchmark_source)
    if not benchmark_order:
        raise EvaluationError(f"{benchmark_source} 不能为空")
    if not isinstance(results_data, dict):
        raise EvaluationError(f"{results_source} 根节点必须是对象")
    result_order, results = _index_by_id(results_data.get("cases"), results_source)

    missing = [case_id for case_id in benchmark_order if case_id not in results]
    extra = [case_id for case_id in result_order if case_id not in benchmark]
    if missing or extra:
        details = []
        if missing:
            details.append(f"缺少结果: {', '.join(missing)}")
        if extra:
            details.append(f"多余结果: {', '.join(extra)}")
        raise EvaluationError(
            f"{benchmark_source} 与 {results_source} 样例未完整配对（" + "; ".join(details) + "）"
        )

    per_case: list[dict[str, Any]] = []
    for case_id in benchmark_order:
        try:
            source_case = benchmark[case_id]
            result_case = results[case_id]
            keywords = _keywords_for(source_case, case_id)
            retrieved_ids = result_case.get("retrievedIds")
            if not isinstance(retrieved_ids, list) or len(retrieved_ids) > 3:
                raise EvaluationError(f"样例 {case_id} 的 retrievedIds 必须是最多 3 项的数组")
            if any(not isinstance(item, str) for item in retrieved_ids):
                raise EvaluationError(f"样例 {case_id} 的 retrievedIds 必须只含字符串")
            if len(set(retrieved_ids)) != len(retrieved_ids):
                raise EvaluationError(f"样例 {case_id} 的 retrievedIds 存在重复项")

            expected_retrieval_id = f"eval-{case_id}"
            retrieval_hit = expected_retrieval_id in retrieved_ids
            stored_retrieval_hit = result_case.get("retrievalHit")
            if not isinstance(stored_retrieval_hit, bool) or stored_retrieval_hit != retrieval_hit:
                raise EvaluationError(
                    f"样例 {case_id} 的 retrievalHit 存档值与 retrievedIds 重算结果不符"
                )

            row: dict[str, Any] = {
                "id": case_id,
                "title": source_case.get("title", result_case.get("title", "")),
                "retrieval_hit": retrieval_hit,
            }
            if not isinstance(row["title"], str):
                raise EvaluationError(f"样例 {case_id} 的 title 必须是字符串")
            for arm in ARMS:
                coverage, seconds = _answer_metrics(result_case.get(arm), keywords, case_id, arm)
                row[f"{arm}_keyword_coverage"] = coverage
                row[f"{arm}_all_keywords_hit"] = coverage == 1.0
                row[f"{arm}_model_response_seconds"] = seconds
            per_case.append(row)
        except EvaluationError as error:
            raise EvaluationError(f"{benchmark_source} / {results_source}: {error}") from error

    case_count = len(per_case)
    hit_count = sum(row["retrieval_hit"] for row in per_case)
    arms: dict[str, Any] = {}
    for arm in ARMS:
        coverages = [row[f"{arm}_keyword_coverage"] for row in per_case]
        response_seconds = [row[f"{arm}_model_response_seconds"] for row in per_case]
        complete_count = sum(row[f"{arm}_all_keywords_hit"] for row in per_case)
        arms[arm] = {
            "mean_per_case_keyword_coverage": _round_finite(_stable_mean(coverages), 4),
            "all_keywords_hit_cases": complete_count,
            "all_keywords_hit_rate": _round_finite(complete_count / case_count, 4),
            "mean_model_response_seconds": _round_finite(_stable_mean(response_seconds), 3),
        }

    return {
        "model": results_data.get("model"),
        "case_count": case_count,
        "retrieval_hit_at_3": {
            "hits": hit_count,
            "rate": round(hit_count / case_count, 4),
        },
        "arms": arms,
        "method": {
            "keyword_coverage": "命中数_i = 大小写不敏感子串匹配的 goldKeyword 数；覆盖率_i = 命中数_i / goldKeyword 总数_i；总体覆盖率 = Σ覆盖率_i / N",
            "retrieval_hit_at_3": "Hit@3 = 预期 eval-{caseId} 出现在前三条 retrievedIds 的样例数 / N",
            "seconds": "平均生成耗时 = Σ原始 seconds_i / N；表示模型回答生成耗时，不是学生解题耗时",
            "limitation": "关键词覆盖是词面代理指标，不等同于专家判定的正确性或学生解决成功率",
        },
        "cases": [
            {
                **row,
                **{
                    f"{arm}_keyword_coverage": _round_finite(row[f"{arm}_keyword_coverage"], 4)
                    for arm in ARMS
                },
            }
            for row in per_case
        ],
    }


def render_text(report: dict[str, Any]) -> str:
    lines = [
        "RAG A/B 基准复算报告",
        f"模型：{report['model'] or '未记录'}；样例数：{report['case_count']}",
        f"Top-3 检索命中：{report['retrieval_hit_at_3']['hits']}/{report['case_count']} "
        f"({report['retrieval_hit_at_3']['rate']:.2%})",
    ]
    for arm, label in (("noRag", "无 RAG"), ("withRag", "有 RAG")):
        metrics = report["arms"][arm]
        lines.append(
            f"{label}：平均关键词覆盖 {metrics['mean_per_case_keyword_coverage']:.4f}；"
            f"全关键词命中 {metrics['all_keywords_hit_cases']}/{report['case_count']}；"
            f"模型回答生成平均耗时 {metrics['mean_model_response_seconds']:.3f} 秒"
        )
    lines.extend((
        f"关键词公式：{report['method']['keyword_coverage']}。",
        f"检索/耗时公式：{report['method']['retrieval_hit_at_3']}；{report['method']['seconds']}。",
        f"限制：{report['method']['limitation']}。",
    ))
    return "\n".join(lines)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--benchmark", type=Path, default=DEFAULT_BENCHMARK,
                        help="benchmark-cases.json 路径")
    parser.add_argument("--results", type=Path, default=DEFAULT_RESULTS,
                        help="保存的 RAG A/B 原始结果 JSON 路径")
    parser.add_argument("--format", choices=("text", "json"), default="text",
                        help="报告格式，默认 text")
    return parser


def main(argv: list[str] | None = None) -> int:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            reconfigure(encoding="utf-8", errors="replace")
    arguments = build_parser().parse_args(argv)
    try:
        report = analyze(
            load_json(arguments.benchmark),
            load_json(arguments.results),
            benchmark_source=str(arguments.benchmark.resolve()),
            results_source=str(arguments.results.resolve()),
        )
    except EvaluationError as error:
        print(f"评估失败：{error}", file=sys.stderr)
        return 1

    if arguments.format == "json":
        print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    else:
        print(render_text(report))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
