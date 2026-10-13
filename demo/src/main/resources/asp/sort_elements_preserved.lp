value(V) :- in(_, V).
value(V) :- out(_, V).
in_count(V, C)  :- value(V), C = #count { P : in(P, V) }.
out_count(V, C) :- value(V), C = #count { P : out(P, V) }.
violation :- value(V), in_count(V, C1), out_count(V, C2), C1 != C2.
#show violation/0.
