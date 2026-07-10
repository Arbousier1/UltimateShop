# 💹EconomyFormat™

EconomyFormat is a configuration format provided by plugins that can associate with your server economy. If an option in a configuration file requires or supports **EconomyFormat**, we will annotate it.&#x20;

{% hint style="info" %}
Please remember: **UltimateShop** is just a shop plugin and does not provide custom economy functionality. If you need a custom economy as your server economy, please find a suitable economy plugin yourself. **Vault** is not an economy plugin, it is just a dependency plugin for many economy plugins. <mark style="color:red;">After installing Vault on the server, it is also necessary to install the economy plugins that support it</mark>.
{% endhint %}

## Hook Economy

Hook economy use format as below:

* economy-plugin: What plugin you want this price economy hook into, for now, **UltimateShop** supports `Vault, GamePoints, PlayerPoints, ExcellentEconomy, UltraEconomy, EcoBits, PEconomy, RedisEconomy, RoyaleEconomy, VotingPlugin`. **Required.**
* economy-type: If the economy plugin supports multiple currencies, enter the currency ID here. **Required if you set a multi-currency plugin, otherwise it's optional.**
* amount: Price Amount. **Required.**
* max-amount: Max amount, useful for dynamic prices. **Optional.**
* min-amount: Min amount, useful for dynamic prices.**Optional.**

**Example:**

```yaml
  1:
    economy-plugin: Vault
    # If you set Economy plugin to ExcellentEconomy, then:
    # economy-plugin: ExcellentEconomy
    # economy-type: Coin
    # Yeah, you need add economy-type option here because its a multi-currency plugin.
    # For this, players will always cost 5$.
    amount: 5
    placeholder: '$5'
```

## Vanilla Economy

Vanilla economy use format as below.

* economy-type: Supports `exp, levels`. **Required.**
* amount: Price Amount. **Required.**
* max-amount: Max amount, useful for dynamic prices. **Optional.**
* min-amount: Min amount, useful for dynamic prices.**Optional.**

**Example:**

```yaml
  1:
    economy-type: levels
    amount: 5
    placeholder: '5 levels'
```
