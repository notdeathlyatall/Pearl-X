
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()"""
toolkit.py - A small collection of everyday Python helpers.

Sections:
    1. String utilities
    2. Number and math utilities
    3. List and collection utilities
    4. File and path utilities
    5. Date and time utilities
    6. Decorators
    7. Simple data structures
    8. Demo / self-test

Run `python toolkit.py` to execute the built-in demo.
"""

from __future__ import annotations

import json
import math
import os
import random
import re
import string
import time
from collections import Counter, deque
from datetime import date, datetime, timedelta
from functools import wraps
from typing import Any, Callable, Iterable, Iterator, Sequence


# ---------------------------------------------------------------------------
# 1. String utilities
# ---------------------------------------------------------------------------

def slugify(text: str) -> str:
    """Convert text into a URL-friendly slug."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


def snake_to_camel(name: str) -> str:
    """Convert snake_case to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def camel_to_snake(name: str) -> str:
    """Convert camelCase to snake_case."""
    step = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", step).lower()


def is_palindrome(text: str) -> bool:
    """Check whether text reads the same forwards and backwards."""
    cleaned = "".join(ch.lower() for ch in text if ch.isalnum())
    return cleaned == cleaned[::-1]


def count_words(text: str) -> dict[str, int]:
    """Return a word-frequency dictionary (case-insensitive)."""
    words = re.findall(r"[a-zA-Z']+", text.lower())
    return dict(Counter(words))


def truncate(text: str, length: int, suffix: str = "...") -> str:
    """Truncate text to `length` characters, adding a suffix if shortened."""
    if len(text) <= length:
        return text
    return text[: max(0, length - len(suffix))] + suffix


def title_case(text: str) -> str:
    """Capitalize each word, keeping small words lowercase (except the first)."""
    small = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for"}
    words = text.lower().split()
    out = []
    for i, w in enumerate(words):
        out.append(w if (w in small and i != 0) else w.capitalize())
    return " ".join(out)


def random_string(length: int = 12, digits: bool = True) -> str:
    """Generate a random alphanumeric string."""
    pool = string.ascii_letters + (string.digits if digits else "")
    return "".join(random.choice(pool) for _ in range(length))


def vowel_count(text: str) -> int:
    """Count vowels in a string."""
    return sum(1 for ch in text.lower() if ch in "aeiou")


def reverse_words(text: str) -> str:
    """Reverse the order of words in a sentence."""
    return " ".join(reversed(text.split()))


def caesar_cipher(text: str, shift: int) -> str:
    """Apply a Caesar cipher. Use a negative shift to decode."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord("A") if ch.isupper() else ord("a")
            result.append(chr((ord(ch) - base + shift) % 26 + base))
        else:
            result.append(ch)
    return "".join(result)


def is_anagram(a: str, b: str) -> bool:
    """Check whether two strings are anagrams of each other."""
    clean = lambda s: sorted(ch.lower() for ch in s if ch.isalnum())
    return clean(a) == clean(b)


# ---------------------------------------------------------------------------
# 2. Number and math utilities
# ---------------------------------------------------------------------------

def is_prime(n: int) -> bool:
    """Return True if n is a prime number."""
    if n < 2:
        return False
    if n < 4:
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    while i * i <= n:
        if n % i == 0 or n % (i + 2) == 0:
            return False
        i += 6
    return True


def primes_up_to(limit: int) -> list[int]:
    """Sieve of Eratosthenes: all primes <= limit."""
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(limit ** 0.5) + 1):
        if sieve[i]:
            for j in range(i * i, limit + 1, i):
                sieve[j] = False
    return [i for i, is_p in enumerate(sieve) if is_p]


def fibonacci(n: int) -> list[int]:
    """Return the first n Fibonacci numbers."""
    seq: list[int] = []
    a, b = 0, 1
    for _ in range(n):
        seq.append(a)
        a, b = b, a + b
    return seq


def factorial(n: int) -> int:
    """Compute n! iteratively."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def gcd(a: int, b: int) -> int:
    """Greatest common divisor (Euclid's algorithm)."""
    while b:
        a, b = b, a % b
    return abs(a)


def lcm(a: int, b: int) -> int:
    """Least common multiple."""
    return abs(a * b) // gcd(a, b) if a and b else 0


def prime_factors(n: int) -> list[int]:
    """Return the prime factorization of n as a list."""
    factors = []
    d = 2
    while d * d <= n:
        while n % d == 0:
            factors.append(d)
            n //= d
        d += 1
    if n > 1:
        factors.append(n)
    return factors


def clamp(value: float, low: float, high: float) -> float:
    """Restrict value to the range [low, high]."""
    return max(low, min(high, value))


def lerp(a: float, b: float, t: float) -> float:
    """Linear interpolation between a and b."""
    return a + (b - a) * t


def mean(values: Sequence[float]) -> float:
    """Arithmetic mean."""
    return sum(values) / len(values)


def median(values: Sequence[float]) -> float:
    """Median of a numeric sequence."""
    s = sorted(values)
    mid = len(s) // 2
    return s[mid] if len(s) % 2 else (s[mid - 1] + s[mid]) / 2


def std_dev(values: Sequence[float]) -> float:
    """Population standard deviation."""
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def to_binary(n: int) -> str:
    """Integer to binary string."""
    return bin(n)[2:]


def human_size(num_bytes: float) -> str:
    """Format a byte count as a readable string (e.g. 1.5 MB)."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if abs(num_bytes) < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} PB"


# ---------------------------------------------------------------------------
# 3. List and collection utilities
# ---------------------------------------------------------------------------

def chunk(items: Sequence[Any], size: int) -> list[Sequence[Any]]:
    """Split a sequence into chunks of the given size."""
    return [items[i : i + size] for i in range(0, len(items), size)]


def flatten(nested: Iterable[Any]) -> list[Any]:
    """Recursively flatten nested lists/tuples."""
    out: list[Any] = []
    for item in nested:
        if isinstance(item, (list, tuple)):
            out.extend(flatten(item))
        else:
            out.append(item)
    return out


def unique(items: Iterable[Any]) -> list[Any]:
    """Remove duplicates while preserving order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def group_by(items: Iterable[Any], key: Callable[[Any], Any]) -> dict[Any, list[Any]]:
    """Group items into a dict using a key function."""
    groups: dict[Any, list[Any]] = {}
    for item in items:
        groups.setdefault(key(item), []).append(item)
    return groups


def partition(items: Iterable[Any], predicate: Callable[[Any], bool]) -> tuple[list, list]:
    """Split items into (matching, non-matching)."""
    yes, no = [], []
    for item in items:
        (yes if predicate(item) else no).append(item)
    return yes, no


def rotate(items: list[Any], k: int) -> list[Any]:
    """Rotate a list right by k positions."""
    if not items:
        return items
    k %= len(items)
    return items[-k:] + items[:-k]


def most_common(items: Iterable[Any], n: int = 1) -> list[tuple[Any, int]]:
    """Return the n most common elements with counts."""
    return Counter(items).most_common(n)


def binary_search(sorted_items: Sequence[Any], target: Any) -> int:
    """Return index of target in a sorted sequence, or -1."""
    lo, hi = 0, len(sorted_items) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if sorted_items[mid] == target:
            return mid
        if sorted_items[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def bubble_sort(items: list[Any]) -> list[Any]:
    """Simple bubble sort (for learning, not production)."""
    arr = list(items)
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


def merge_sort(items: list[Any]) -> list[Any]:
    """Merge sort implementation."""
    if len(items) <= 1:
        return list(items)
    mid = len(items) // 2
    left, right = merge_sort(items[:mid]), merge_sort(items[mid:])
    merged, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def merge_dicts(*dicts: dict) -> dict:
    """Merge dictionaries left to right (later ones win)."""
    result: dict = {}
    for d in dicts:
        result.update(d)
    return result


def invert_dict(d: dict) -> dict:
    """Swap keys and values."""
    return {v: k for k, v in d.items()}


# ---------------------------------------------------------------------------
# 4. File and path utilities
# ---------------------------------------------------------------------------

def read_text(path: str, encoding: str = "utf-8") -> str:
    """Read an entire text file."""
    with open(path, "r", encoding=encoding) as f:
        return f.read()


def write_text(path: str, content: str, encoding: str = "utf-8") -> None:
    """Write text to a file, creating parent folders if needed."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding=encoding) as f:
        f.write(content)


def read_json(path: str) -> Any:
    """Load JSON from a file."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any, indent: int = 2) -> None:
    """Save data as pretty-printed JSON."""
    write_text(path, json.dumps(data, indent=indent, ensure_ascii=False))


def count_lines(path: str) -> int:
    """Count lines in a text file."""
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for _ in f)


def list_files(folder: str, extension: str | None = None) -> list[str]:
    """Recursively list files in a folder, optionally filtered by extension."""
    found = []
    for root, _dirs, files in os.walk(folder):
        for name in files:
            if extension is None or name.endswith(extension):
                found.append(os.path.join(root, name))
    return sorted(found)


def folder_size(folder: str) -> int:
    """Total size of all files in a folder, in bytes."""
    total = 0
    for root, _dirs, files in os.walk(folder):
        for name in files:
            try:
                total += os.path.getsize(os.path.join(root, name))
            except OSError:
                pass
    return total


# ---------------------------------------------------------------------------
# 5. Date and time utilities
# ---------------------------------------------------------------------------

def today_str(fmt: str = "%Y-%m-%d") -> str:
    """Today's date as a formatted string."""
    return date.today().strftime(fmt)


def days_between(start: date, end: date) -> int:
    """Number of days between two dates."""
    return abs((end - start).days)


def add_days(start: date, days: int) -> date:
    """Return a date shifted by the given number of days."""
    return start + timedelta(days=days)


def is_weekend(d: date) -> bool:
    """True if the date falls on Saturday or Sunday."""
    return d.weekday() >= 5


def is_leap_year(year: int) -> bool:
    """Leap year check."""
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def age_from_birthdate(born: date, today: date | None = None) -> int:
    """Compute age in whole years."""
    today = today or date.today()
    years = today.year - born.year
    if (today.month, today.day) < (born.month, born.day):
        years -= 1
    return years


def format_duration(seconds: float) -> str:
    """Format seconds as e.g. '1h 02m 05s'."""
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    if h:
        return f"{h}h {m:02d}m {s:02d}s"
    if m:
        return f"{m}m {s:02d}s"
    return f"{s}s"


def parse_date(text: str) -> date:
    """Parse a date from a few common formats."""
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError(f"Unrecognized date format: {text!r}")


# ---------------------------------------------------------------------------
# 6. Decorators
# ---------------------------------------------------------------------------

def timer(func: Callable) -> Callable:
    """Print how long a function takes to run."""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[timer] {func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper


def retry(times: int = 3, delay: float = 0.5) -> Callable:
    """Retry a function if it raises an exception."""
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    print(f"[retry] attempt {attempt}/{times} failed: {exc}")
                    time.sleep(delay)
            raise last_error  # type: ignore[misc]
        return wrapper
    return decorator


def memoize(func: Callable) -> Callable:
    """Cache results of a function by its arguments."""
    cache: dict = {}

    @wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


# ---------------------------------------------------------------------------
# 7. Simple data structures
# ---------------------------------------------------------------------------

class Stack:
    """Last-in, first-out stack."""

    def __init__(self) -> None:
        self._items: list[Any] = []

    def push(self, item: Any) -> None:
        self._items.append(item)

    def pop(self) -> Any:
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self) -> Any:
        if not self._items:
            raise IndexError("peek at empty stack")
        return self._items[-1]

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Stack({self._items})"


class Queue:
    """First-in, first-out queue."""

    def __init__(self) -> None:
        self._items: deque = deque()

    def enqueue(self, item: Any) -> None:
        self._items.append(item)

    def dequeue(self) -> Any:
        if not self._items:
            raise IndexError("dequeue from empty queue")
        return self._items.popleft()

    def is_empty(self) -> bool:
        return not self._items

    def __len__(self) -> int:
        return len(self._items)

    def __repr__(self) -> str:
        return f"Queue({list(self._items)})"


class LinkedList:
    """Minimal singly linked list."""

    class _Node:
        def __init__(self, value: Any) -> None:
            self.value = value
            self.next: LinkedList._Node | None = None

    def __init__(self) -> None:
        self.head: LinkedList._Node | None = None
        self._size = 0

    def append(self, value: Any) -> None:
        node = self._Node(value)
        if not self.head:
            self.head = node
        else:
            cur = self.head
            while cur.next:
                cur = cur.next
            cur.next = node
        self._size += 1

    def __iter__(self) -> Iterator[Any]:
        cur = self.head
        while cur:
            yield cur.value
            cur = cur.next

    def __len__(self) -> int:
        return self._size

    def __repr__(self) -> str:
        return " -> ".join(str(v) for v in self) or "(empty)"


class Counter2D:
    """Tiny grid counter, handy for simple games and simulations."""

    def __init__(self, width: int, height: int) -> None:
        self.width, self.height = width, height
        self.grid = [[0] * width for _ in range(height)]

    def increment(self, x: int, y: int, amount: int = 1) -> None:
        self.grid[y][x] += amount

    def total(self) -> int:
        return sum(sum(row) for row in self.grid)

    def __repr__(self) -> str:
        return "\n".join(" ".join(f"{c:2d}" for c in row) for row in self.grid)


# ---------------------------------------------------------------------------
# 8. Demo / self-test
# ---------------------------------------------------------------------------

def _demo() -> None:
    print("== Strings ==")
    print(slugify("Hello, World! This is a Test"))
    print(snake_to_camel("make_big_file"), camel_to_snake("makeBigFile"))
    print(is_palindrome("A man, a plan, a canal: Panama"))
    print(caesar_cipher("Hello", 3), caesar_cipher(caesar_cipher("Hello", 3), -3))

    print("\n== Math ==")
    print(primes_up_to(30))
    print(fibonacci(10))
    print(gcd(48, 18), lcm(4, 6), prime_factors(360))
    print(mean([1, 2, 3, 4]), median([5, 1, 3]), round(std_dev([2, 4, 4, 4, 5, 5, 7, 9]), 2))
    print(human_size(1_572_864))

    print("\n== Collections ==")
    print(chunk([1, 2, 3, 4, 5], 2))
    print(flatten([1, [2, [3, 4]], 5]))
    print(unique([3, 1, 3, 2, 1]))
    print(merge_sort([5, 2, 9, 1, 7]))
    print(binary_search([1, 3, 5, 7, 9], 7))

    print("\n== Dates ==")
    print(is_leap_year(2024), format_duration(3725))
    print(age_from_birthdate(date(2000, 5, 17), date(2026, 11, 24)))

    print("\n== Data structures ==")
    s = Stack()
    for i in range(3):
        s.push(i)
    print(s, s.pop())
    ll = LinkedList()
    for v in ("a", "b", "c"):
        ll.append(v)
    print(ll)

    @timer
    @memoize
    def slow_square(n: int) -> int:
        time.sleep(0.05)
        return n * n

    print(slow_square(12))
    print(slow_square(12))


if __name__ == "__main__":
    _demo()
