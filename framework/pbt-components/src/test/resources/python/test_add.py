def add(a, b):
    return a + b


def buggy_add(a, b):
    return a - b


def fail_on_negative(n):
    if n < 0:
        raise ValueError("negative")
    return n
