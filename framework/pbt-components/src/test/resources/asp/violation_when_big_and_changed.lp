#include "shared_big.lp".
violation :- big, in(V), out(W), V != W.
#show violation/0.
