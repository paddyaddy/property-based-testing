% Derives violation when no out/2 fact exists (i.e. SUT output is absent).
% Used to verify the ASP validator reports a failure when SUT output is null.
violation :- not out(_, _).

#show violation/0.
