import pytest

from harnesses.common.provider import ProviderError, parse_json_object


def test_parse_json_object_accepts_json_followed_by_extra_text():
    assert parse_json_object('{"status":"ready"} extra text') == {"status": "ready"}


def test_parse_json_object_rejects_non_object():
    with pytest.raises(ProviderError, match="invalid JSON"):
        parse_json_object('[1, 2, 3]')
