def reference_sort(xs):
    return sorted(xs)


def buggy_sort(xs):
    # BUG: drops last element before sorting
    return sorted(xs[:-1]) if xs else []


def buggy_sort_reverse(xs):
    # BUG: reverses instead of sorting — clearly breaks the ordered invariant
    return list(reversed(xs))


def sort_unless_negative_first(xs):
    # Hangs on inputs starting with a negative number, to exercise the per-call timeout.
    if xs and xs[0] < 0:
        import time
        time.sleep(60)
    return sorted(xs)
