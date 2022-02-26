
# MochiJump Transaction Reviewer

This project attempts to accurately analyze transaction information from on
chain data as well as centralized exchanges to build a picture of what actually happened with regard to your coins in a
given period of time. It does this by first compiling a list of transactions organized by time and then labeled with a verb
`BUY`, `SELL`, or `LOSE` and then creating an ownership state detailing what is still owned by the user and what cost basis was
used in the event of a sell, loss, trade etc. The software will spit out two files representing each of these states.

*I AM NOT A TAX SPECIALIST, NOR IS THIS SOFTWARE TAX SOFTWARE. THIS SOFTWARE OFFERS NO GUARANTEES WITH REGARD TO ANYTHING AND IS NOT LIABLE FOR ANY LOSSES INCURRED BY USING IT*

*THIS PROJECT IS EXTREMELY EARLY. PLEASE READ THROUGH IT'S USAGE AND CURRENT STATE BEFORE ATTEMPTING TO USE*

Feedback is greatly appreciated!

- [Attention](#attention)
	- [This is a Java Application](#this-is-a-java-application)
	- [No Support For NFT history at this time](#no-support-for-nft-history-at-this-time)
	- [Overrides Required](#overrides-required)
	- [Incoming ERC20 transactions resulting from non verified contracts](#incoming-erc20-transactions-resulting-from-non-verified-contracts)
- [Outstanding Issues](#outstanding-issues)
	- [Normalize hashes](#normalize-hashes)
	- [Configs and NPEs](#configs-and-npes)
	- [Gemini CSV Reader](#gemini-csv-reader)
	- [Loans](#loans)
		- [Borrowing and Repaying from AAVE](#borrowing-and-repaying-from-aave)
		- [Bento Box and Kashi](#bento-box-and-kashi)
	- [Balancer](#balancer)
	- [Further build testing required](#further-build-testing-required)
- [Design](#design)
	- [Core Concepts and Extensibility](#core-concepts-and-extensibility)
		- [ChainStateAnalyzer](#chainstateanalyzer)
		- [HistoricalPriceLookUp](#historicalpricelookup)
	- [Configuration](#configuration)
		- [Wallet](#wallet)
		- [txNativeBuyReturnMissing](#txnativebuyreturnmissing)
- [Tips](#tips)

## Attention:

### This is a Java Application

This software is made using java 11. At this time I have not uploaded the built jar anywhere, so to run this software,
you'll need to build, or run, it with maven. To make this more accessible the jar should be deployed somewhere so 
less technical people can run it more easily.

Ideally before this is done include a wrapper that asks for and sets variables, maybe builds the config, and then runs
the jar

### No Support for NFT history at this time

I have no experience reviewing NFT transactions, so this first iteration was not built to support analyzing NFT 
transactions. This would make a good candidate for a future feature.

### Overrides Required

There are a number of cases that require a config override. The application is designed to alert you if it detects a 
scenario which requires one along with instructions.

The core of this stems from the fact that [ether/polygon]scan drops relevant information in certain cases. Where 
identified by noticing a specific patten or usage of a field, we prompt an `IllegalStateException` detailing what needs 
to be configured and how, which will involve a manual step of looking up something on etherscan that is not available 
via api as far as I am aware.

A major recurring theme of this is where a transaction record in csv format will just be missing information regarding 
the transfer of native tokens.

As this project is very new it is important to highlight that it is possible that the override predicates miss or 
incorrectly match for a given patten of on chain data. This will remain the case for new scenarios until the current predicates
are extensively tested.

### Incoming ERC20 transactions resulting from non verified contracts

This app in its current form ignores incoming erc20 transactions from non verified contracts. This was first noticed 
while inspecting transaction details of a gemini transfer to wallet. Since gemini does not charge for transfers there 
is no loss to record from these transactions, so for this first iteration we are ignoring these transactions entirely 
(including the gas cost for sending)

There are ways to override this behavior, if you want to count the gas for instance, you can override the 
`com.mochijump.global.txNativeLossOverride` value to the gas price per tx (see Design.Configurations below).

## Outstanding Issues:

There are plenty of todo's and cleanup already in the code, however, high priority issues not already addressed in this
 readme will be called out here


### Normalize hashes
Discovered by reddit user {{REDACTED_ASK_FOR_PERMISSION_TO_USE_NAME}}, we should normalize all hashes (all lower case 
sounds good). 

### Configs and NPEs
Should ensure that non configured but needed configurations fail with a more helpful message than a NPE.

### Gemini CSV Reader
This first iteration had an incorrect assumption discovered by reddit user {{REDACTED_ASK_FOR_PERMISSION_TO_USE_NAME}}. 
The csv created by gemini
is not static. The later columns pertaining to coin values coming in and out are not standard and this software should 
programmatically determine where to look based on the first row's `X Amount X` columns

### Loans

#### Borrowing and Repaying from AAVE

This app does not have a good way of handling this currently. It treats both as 0 cost basis, but this has tax 
implications if value is held for a long period of time. Ideally it should go on the books as inventory only and 
completely ignore the value, this might require a new verb.

#### Bento Box and Kashi

I'm lacking domain knowledge around how these contracts actually work. This might require it's own state to keep track 
of (i.e. how much in vs how much taken out)

### Balancer

These transactions need to be reviewed. I have no experience with balancer, but we might need a new predicate to
identify how balancer works.

### Further build testing required 

I had the luxury of working with my own data when creating this first iteration. So that means I was able to perform
good environmental tests. That does not excuse the lack of tests as part of the build. A better test suite is always
recommended

## Balancer

These transactions need to be reviewed. I have no experience with balancer, but we might need a new predicate to
identify how balancer works.

## Design

### Core Concepts and Extensibility

When creating this project I wanted it to be easily adapted and have new features added for. This project 
attempts to assemble a master list of all transactions that occurred during the given time frame, and then proceeds to
build a state of ownership using those transactions.

There is a central and simple flow that makes use of the four tenant interfaces of this application:
1. `CexReader`
2. `ChainReader`
3. `HistoricalPriceLookUp`
4. `StateMaintainer`
5. `ChainStateAnalyzer`

If new behavior is wanted at any of these levels, you can create a new class that implements these interfaces and 
basically just inject that implementation instead of the default which is done in the main class 
`TransactionReviewerApplication` (TODO Config can make this a lot better than doing it manually)

The flow of the logic can be seen in the `Orchestrater`. It starts by attempting to read any already existing state
(not implemented yet) via the `StateMaintainer` and then proceeds to begin adding to the master list via the `CexReader`.
This is the simplest to analyze as Centralized Exchanges clearly label buy and sell information

The next stage is working with on-chain data this done via a combination of the 3 of these coupled concepts:
`ChainReader`, `HistoricalPriceLookUp`, `ChainStateAnalyzer`. Together the three of these are able to determine what 
actually happened within a single transaction. Consider for example a case where we trade a native token for an erc20.
This would be a sell and a buy. In order to determine what's happening we need to ChainData to read from as well as the 
historical price as well as a way to analyze what that chain data actually means

Finally the `StateMaintainer` has the unenviable job of making sure every request regarding state is recorded properly 
as well as converting the master list into the current state of ownership.

#### ChainStateAnalyzer

My first attempt is to create a situation where the application asks the user what it needs. This was done 
by creating a system of predicates to identify certain patterns, and in the event that none or multiples matched, then 
it would throw an exception detailing where it went wrong and what transaciton hash to look at.

Unfortunately due to available data being lacking in some places I had to include predicates that if matched do not 
attempt to match later predicates. 

The major current risk is that there is some collision with an already existing rule with something that indeed needs 
a new one (override or not)

#### HistoricalPriceLookUp

This is a major bottleneck for the application. Calling coingecko and being rate limited means this guy can run for 10 
minutes or more when dealing with thousands of transactions

### Configuration

The app is configurable so that users can make use of overrides for their own needs.
The easiest way to setup your own configuration is to set a environmental variable to the absolute location of your own
`properties.yaml` for example in bash:

```text
export SPRING_CONFIG_LOCATION=file:/Users/SomeUser/Documents/EthMatic2021Overrides.yaml
```

Below is a sample complete configuration:

```yaml
mochijump.global:
  # Default values, no need to redefine unless changing
  harvestRewardsMethodNames:
    - "Get Reward"
    - "Harvest"
    - "Claim Rewards"
    - "Get Reward"
    - "Harvest"
    - "Claim Rewards"
  exitLpMethodNames:
    - "Exit"
    - "Withdraw"
    - "Withdraw And Harvest"
  methodsThatCauseMissingInfo:
    - "Swap Exact Tokens For ETH"
    - "Swap Tokens For Exact ETH"
  # User defined values
  # Wallet will always need to be defined
  wallet: "0xYourWallet"
  txNativeLossOverride:
    "0xTxHash": 0.1583
    "0xTxHash2": 0.111
  txNativeBorrowOverride:
    "0xTxHash": 2D #D is just to identify whole numbers as doubles
  txNativeCollateralWithdrawOverride:
    "0xTxHash": 36D
  txNativeAmountFromLpExit:
    "0xTxHash": 3678.410660309696054541
  txNativeBuyReturnMissing:
    "0xTxHash": 3D
  contractIgnoreCompletely:
    - "0xContractAddress1"
  txIgnoreCompletely:
    - "0xTxHash1"
    - "0xTxHash2"
  originationWallets:
    - "0xWalletOrContractAddress"
  geminiCsvLoc: "/ABS/PATH/TO/GEMINI.csv"
  onChainCsvFiles:
    "ETH":
      - "/ABS/PATH/TO/ETHERSCAN-TX.csv"
      - "/ABS/PATH/TO/ETHERSCAN-ERC20-TX.csv"
    "MATIC":
      - "/ABS/PATH/TO/POLYGONSCAN-TX.csv"
      - "/ABS/PATH/TO/POLYGONSCAN-ERC20-TX.csv"
      - "/ABS/PATH/TO/POLYGONSCAN-ERC20-TX-SECONO-FILE.csv"
```

#### Wallet

This is your wallet, you must enter it via config here or as an environmental variable `MOCHIJUMP_GLOBAL_WALLET`

#### txNativeBuyReturnMissing

This config is for transactions where a trade was made for a native token, however, the amount incoming of the native 
coin is missing from [ether/polygon]scan's csv file (it shows up as 0 incoming)

```yaml
mochijump.global:
  txNativeBuyReturnMissing:
      "0xTxHash01": 3D #D ensures unboxing as a Double
      "0xTxHash02": 17.3993845 
```

#### Other Config

TODO write up the use cases of the other overrides. In the meantime you can search the project to find where these configs
 are used in code.

## Tips
If you found this software useful and want to give a tip, it is greatly appreciated! Feel free to send tips to 
0x5EAf6F7aeff7326Ba33fD2CbfdBbaFCC55922a23
