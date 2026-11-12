violation :- in(N), N < 0, not exception("java.lang.NumberFormatException").
violation :- in(N), N >= 0, exception(_).
#show violation/0.
