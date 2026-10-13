in_len(N)  :- N = #count { P : in(P, _) }.
out_len(M) :- M = #count { P : out(P, _) }.
violation :- in_len(N), out_len(M), N != M.
#show violation/0.
