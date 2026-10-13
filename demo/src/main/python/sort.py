def reference_sort(xs):
    return sorted(xs)

def buggy_sort(xs):
    """BUG: sorts in descending order."""
    return sorted(xs, reverse=True)
