"""Deterministic local embedding and cosine similarity utilities."""

from __future__ import annotations

import hashlib
import math
import re


class EmbeddingSimilarity:
    def __init__(self, dimensions: int = 96) -> None:
        self.dimensions = dimensions

    def embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        for token in self._tokens(text):
            for ngram in self._ngrams(token):
                index = int(hashlib.sha1(ngram.encode("utf-8")).hexdigest(), 16) % self.dimensions
                vector[index] += 1.0
        norm = math.sqrt(sum(value * value for value in vector))
        if norm:
            vector = [round(value / norm, 6) for value in vector]
        return vector

    def similarity(self, left: str | list[float], right: str | list[float]) -> float:
        left_vector = left if isinstance(left, list) else self.embed(left)
        right_vector = right if isinstance(right, list) else self.embed(right)
        if not left_vector or not right_vector:
            return 0.0
        dot = sum(a * b for a, b in zip(left_vector, right_vector))
        left_norm = math.sqrt(sum(value * value for value in left_vector))
        right_norm = math.sqrt(sum(value * value for value in right_vector))
        if not left_norm or not right_norm:
            return 0.0
        return round(dot / (left_norm * right_norm), 4)

    @staticmethod
    def _tokens(text: str) -> list[str]:
        return re.findall(r"[\u4e00-\u9fff]{1,4}|[A-Za-z][A-Za-z0-9_+-]{1,}", str(text).lower())

    @staticmethod
    def _ngrams(token: str) -> list[str]:
        if len(token) <= 2:
            return [token]
        return [token[index : index + 2] for index in range(len(token) - 1)]
