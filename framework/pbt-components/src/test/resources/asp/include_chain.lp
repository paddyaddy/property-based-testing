% includes the same file twice, directly and through another include
#include "shared_big.lp".
#include "skip_unless_big.lp".
violation :- big, out(0).
#show violation/0.
