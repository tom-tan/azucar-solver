# Azucar: a SAT-based CSP solver based on "compact order encoding"

Azucar is a SAT-based CSP solver which is an enhancement version of [Sugar](http://bach.istc.kobe-u.ac.jp/sugar/) which is an award-winning system of GLOBAL categories of the 2008 and 2009 International CSP solver competitions.
It can solve finite non-linear Constraint Satisfaction Problems (CSP), Constraint Optimization Problems (COP), and Max-CSP over integers.

Azucar uses a new SAT-encoding method named compact order
encoding. In the compact order encoding, each integer is represented
by using a numeral system of any base, and each digit-wise comparison
is encoded by using order encoding.  In the order encoding, a
comparison `x <= a` is encoded by a different Boolean variable for each
integer variable `x` and integer value `a`.
