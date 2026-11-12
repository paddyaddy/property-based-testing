from ._sequences import reversed_text


def reverse(s):
    return reversed_text(s)


def buggy_reverse(s):
    # keeps the last character in place
    return reversed_text(s[:-1]) + s[-1:]
