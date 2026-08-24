# ➗ Math Calculate Format

UltimateShop evaluates numeric expressions directly with **Sparrow Expression 1.0**. The
expression is compiled after UltimateShop and PlaceholderAPI values have been replaced.
There is no legacy expression conversion layer.

{% hint style="info" %}
Enable `math.enabled` in `config.yml` before using expressions.
{% endhint %}

`Sparrow` uses `double` internally. UltimateShop applies `math.scale` to the final
result, but expressions should not be used for integers that require more than 15–16
significant digits or for exact high-precision accounting.

## Quick examples

`amount` can contain a complete Sparrow expression:

```yaml
buy-prices:
  1:
    economy-type: exp
    amount: "MAX(1, 5 + ({buy-times-server} - {sell-times-server}) * 0.2)"

sell-limits:
  global: "FLOOR(100 / (1 + {sell-times-server}))"
```

PlaceholderAPI values are replaced first:

```yaml
amount: "%vault_eco_balance% * 0.05"
```

## Values and constants

| Form | Example | Notes |
|---|---|---|
| Decimal | `12`, `3.5`, `.25` | Stored as `double` |
| Scientific notation | `1.5e2`, `2E-3` | Signed exponents are supported |
| Hexadecimal | `0xFF` | Converted to `double` |
| Constants | `TRUE`, `FALSE`, `PI`, `E` | Names are case-insensitive |
| Grouping | `(2 + 3) * 4` | Parentheses may be nested |

Unknown identifiers are rejected. UltimateShop placeholders such as
`{buy-times-player}` must be replaced with numbers before Sparrow compiles the expression.
The only expression variable supplied by UltimateShop is `i`, and it is available only
inside a `SIGMA` body.

## Operators

From highest to lowest precedence:

| Operators | Example |
|---|---|
| Function calls and parentheses | `MAX(10, (2 + 3) * 4)` |
| Power `^` | `2 ^ 10` |
| Unary `+`, `-`, `!`, `NOT` | `NOT(FALSE)` |
| `*`, `/`, `%` | `10 % 3` |
| `+`, `-` | `10 - 3 + 2` |
| `>`, `>=`, `<`, `<=` | `12 >= 10` |
| `=`, `==`, `!=`, `<>` | `1 == 1` |
| `&`, `&&`, `AND` | `1 < 2 AND 2 < 3` |
| <code>&#124;</code>, <code>&#124;&#124;</code>, `OR` | `FALSE OR TRUE` |

Comparisons and logical operators return `1` or `0`. UltimateShop also enables
Sparrow's native implicit multiplication option, so `2(3 + 4)` and `2PI` are accepted.
Using an explicit `*` is still recommended in configuration files for readability.

## Common numeric functions

Every function call requires parentheses. Function names are case-insensitive.

| Function | Example |
|---|---|
| Absolute value | `ABS(-5)` |
| Square/cube root | `SQRT(16)`, `CBRT(27)` |
| Rounding | `ROUND(12.36, 1)`, `FLOOR(3.9)`, `CEIL(3.1)` |
| Exponential/logarithm | `EXP(1)`, `LOG(E)`, `LOG10(100)` |
| Power/factorial | `POW(2, 3)`, `FACT(5)` |
| Aggregate | `SUM(1, 2, 3)`, `AVERAGE(2, 4, 6)` |
| Bounds | `MIN(8, 3, 5)`, `MAX(8, 3, 5)`, `CLAMP(12, 0, 10)` |
| Conditional | `IF(1 < 2, 100, 0)` |
| Selection | `SWITCH(2, 1, 100, 2, 200, 0)` |
| Random | `RANDOM()`, `RANDOM(5, 15)`, `RANDOM_INT(1, 7)` |
| Probability | `IF(CHANCE(0.25), 2, 1)` |

Sparrow also supplies `SIGN`, `HYPOT`, `LERP`, `INVERSE_LERP`, `SMOOTHSTEP`,
`FMA`, `APPROX_EQ`, `IS_FINITE`, and `IS_NAN`.

## Trigonometric functions

All native trigonometric inputs and inverse-function outputs use **radians**:

```text
SIN(PI / 2)
COS(PI)
TAN(PI / 4)
ASIN(1)
ATAN2(1, 1)
```

Use `RAD` and `DEG` for explicit conversion:

```text
SIN(RAD(90))
RAD(180)
DEG(PI)
```

Additional native functions include `COT`, `CSC`, `SEC`, `ACOT`, `SINH`,
`COSH`, `TANH`, `ASINH`, `ACOSH`, `ATANH`, `COTH`, `CSCH`, `SECH`,
and `ACOTH`.

## Custom numeric function: SIGMA

UltimateShop registers `SIGMA` through Sparrow's native `FunctionBinder`:

```text
SIGMA(start, end, body)
```

The range is inclusive and the body is a numeric Sparrow expression. Use `i` as the
current index:

```text
SIGMA(1, 10, i)                         = 55
SIGMA(1, 10, i * i)                     = 385
SIGMA(1, 10, IF(i % 2 == 0, i, 0))      = 30
1 + SIGMA(1, 3, i)                      = 7
SIGMA(1, 3, SIGMA(1, i, i))             = 10
```

The body is **not a string**. Do not write `SIGMA(1, 10, "i * i")`.

Safety limits:

- at most 100,000 iterations for one `SIGMA` call;
- at most 1,000,000 total iterations in one complete expression;
- at most 32 nested `SIGMA` calls;
- non-finite bounds, body values, sums, and final results are rejected.

## Migration from older formula examples

Use the native Sparrow form on the right:

| Old form | Native Sparrow form |
|---|---|
| `abs-1` or `abs$1` | `ABS(-1)` |
| `round1.5` | `ROUND(1.5)` |
| `ceil1.05` | `CEIL(1.05)` |
| `rand4` | `RANDOM(0, 4)` |
| `sin$2` | `SIN(2)` |
| `SIGMA(1, 10, "i")` | `SIGMA(1, 10, i)` |

`SINR` aliases, `NULL`, and `COALESCE` are not registered. Use Sparrow's native
functions and numeric expressions directly.
