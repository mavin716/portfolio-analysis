package com.wise.portfolio.fund;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.service.CurrencyHelper;

public class PortfolioFund extends MutualFund {

	private boolean isClosed = false;
	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	private double shares = 0;
	private BigDecimal preSpent = BigDecimal.ZERO;
	private String oldFundSymbol;
	private PortfolioFund oldFund;
	private LocalDate oldFundConverted;
	public LocalDate getOldFundConverted() {
		return oldFundConverted;
	}

	private boolean isFixedExpensesAccount = false;

	public boolean isFixedExpensesAccount() {
		return isFixedExpensesAccount;
	}

	public void setFixedExpensesAccount(boolean fixedExpensesAccount) {
		this.isFixedExpensesAccount = fixedExpensesAccount;
	}

	public PortfolioFund getOldFund(Portfolio portfolio) {
		if (oldFundSymbol != null && oldFund == null) {
			oldFund = portfolio.getFund(oldFundSymbol);
		}
		return oldFund;
	}

	public void setLinkedFundSymbol(String linkedFundSymbol) {
		this.oldFundSymbol = linkedFundSymbol;
	}

	public BigDecimal getPreSpent() {
		return preSpent;
	}

	public void setPreSpent(BigDecimal preSpent) {
		this.preSpent = preSpent;
	}

	protected Map<LocalDate, Collection<Transaction>> income = new TreeMap<>();
	protected Map<LocalDate, Collection<Transaction>> distributions = new TreeMap<>();
	protected Map<LocalDate, Collection<Transaction>> withdrawals = new TreeMap<>();
	protected Map<LocalDate, Collection<Transaction>> exchanges = new TreeMap<>();
	protected Map<LocalDate, Collection<Transaction>> shareConversions = new TreeMap<>();

	public double getShares() {
		return shares;
	}

	public void setShares(double d) {
		this.shares = d;
	}

	public BigDecimal getValue() {
		return currentPrice.multiply(new BigDecimal(shares)).setScale(2, BigDecimal.ROUND_UP);
	}

	public BigDecimal getAvailableValue() {
		BigDecimal value = currentPrice.multiply(new BigDecimal(shares)).setScale(2, BigDecimal.ROUND_UP);
		if (preSpent.compareTo(BigDecimal.ZERO) > 0) {
			value = value.subtract(preSpent);
		}
		return value;
	}

	public BigDecimal getValueByCategory(FundCategory category) {
		BigDecimal value = BigDecimal.ZERO;

		BigDecimal categoryPercentage = categoriesMap.get(category);
		if ((categoryPercentage.compareTo(BigDecimal.ZERO) > 0) && (currentPrice.compareTo(BigDecimal.ZERO) > 0)) {
			value = currentPrice.multiply(new BigDecimal(shares)).multiply(categoryPercentage).setScale(2,
					BigDecimal.ROUND_HALF_UP);
		}

		return value;
	}

	public void addIncome(LocalDate transactionDate, String transactionType, Float transactionShares,
			BigDecimal transactionSharePrice, BigDecimal transastionPrincipal, String transactionSourceFile) {
		Transaction newTransaction = new Transaction(transactionDate, getSymbol(), transactionType, transactionShares,
				transactionSharePrice, transastionPrincipal, transactionSourceFile);

		Collection<Transaction> transactionsForDate = income.get(transactionDate);
		if (transactionsForDate != null) {
			for (Transaction transaction : transactionsForDate) {
				if (transaction.getTransactionType().contentEquals(transactionType)) {
					// duplicate
					return;
				}
			}
		} else {
			transactionsForDate = new ArrayList<Transaction>();
			income.put(transactionDate, transactionsForDate);
		}
		transactionsForDate.add(newTransaction);
//		System.out.println(transactionDate + " : "
//				+ String.format("%-12s", CurrencyHelper.formatAsCurrencyString(BigDecimal.ZERO.subtract(transastionPrincipal))) + " "
//				+ String.format("%-24s", transactionType) + ": " + getShortName());
		
	}
	public void addDistribution(LocalDate transactionDate, String transactionType, Float transactionShares,
			BigDecimal transactionSharePrice, BigDecimal transastionPrincipal, String transactionSourceFile) {

		Transaction newTransaction = new Transaction(transactionDate, getSymbol(), transactionType, transactionShares,
				transactionSharePrice, transastionPrincipal, transactionSourceFile);

		Collection<Transaction> transactionsForDate = distributions.get(transactionDate);
		if (transactionsForDate != null) {
			for (Transaction transaction : transactionsForDate) {
				if (transaction.getTransactionType().contentEquals(transactionType)) {
					// duplicate
					return;
				}
			}
		} else {
			transactionsForDate = new ArrayList<Transaction>();
			distributions.put(transactionDate, transactionsForDate);
		}
		transactionsForDate.add(newTransaction);
//		System.out.println(transactionDate + " : "
//				+ String.format("%-12s", CurrencyHelper.formatAsCurrencyString(BigDecimal.ZERO.subtract(transastionPrincipal))) + " "
//				+ String.format("%-24s", transactionType) + ": " + getShortName());
	}

	public BigDecimal getDistributionsForDate(LocalDate date) {
		BigDecimal transactionsAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : distributions.entrySet()) {
			if (entry.getKey().isEqual(date)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					transactionsAmount = transactionsAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return transactionsAmount.negate();
	}

	public BigDecimal getDistributionsAfterDate(LocalDate date) {
		BigDecimal transactionsAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : distributions.entrySet()) {
			if (!entry.getKey().isBefore(date)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					transactionsAmount = transactionsAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return transactionsAmount.negate();
	}

	public BigDecimal getDistributionsBetweenDates(LocalDate startDate, LocalDate endDate) {
		BigDecimal transactionsAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : distributions.entrySet()) {
			if (entry.getKey().isAfter(startDate) && entry.getKey().isBefore(endDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					transactionsAmount = transactionsAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return transactionsAmount.negate();
	}

	public BigDecimal getWithdrawalTotalForDate(LocalDate date) {
		BigDecimal withdrawalAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : withdrawals.entrySet()) {
			if (entry.getKey().isEqual(date)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					withdrawalAmount = withdrawalAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return withdrawalAmount;
	}

	public BigDecimal geWithdrawalsBetweenDates(LocalDate startDate, LocalDate endDate) {
		BigDecimal transactionsAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : withdrawals.entrySet()) {
			if (entry.getKey().isAfter(startDate) && entry.getKey().isBefore(endDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					transactionsAmount = transactionsAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return transactionsAmount.negate();
	}

	public BigDecimal getWithdrawalsUpToDate(LocalDate date) {
		BigDecimal withdrawalAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : withdrawals.entrySet()) {
			if (!entry.getKey().isBefore(date)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					withdrawalAmount = withdrawalAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return withdrawalAmount;
	}

	public BigDecimal getExchangeTotalFromDate(LocalDate date) {
		BigDecimal exchangeAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : exchanges.entrySet()) {
			if (!entry.getKey().isBefore(date)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					exchangeAmount = exchangeAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return exchangeAmount;
	}

	public void addWithdrawal(LocalDate transactionDate, String transactionType, Float transactionShares,
			BigDecimal transactionSharePrice, BigDecimal transastionPrincipal, String transactionSourceFile) {

		Transaction newTransaction = new Transaction(transactionDate, getSymbol(), transactionType, transactionShares,
				transactionSharePrice, transastionPrincipal, transactionSourceFile);

		Collection<Transaction> transactionsForDate = withdrawals.get(transactionDate);
		if (transactionsForDate != null) {
			for (Transaction transaction : transactionsForDate) {
				if (transaction.getTransactionType().contentEquals(transactionType) 
						&& !transaction.getTransactionSourceFile().contentEquals(transactionSourceFile)) {
					// duplicate, sell into two different accounts
					return;
				}
			}
		} else {
			transactionsForDate = withdrawals.get(transactionDate.minusDays(1));
			if (transactionsForDate != null) {
				for (Transaction transaction : transactionsForDate) {
					if (transaction.getTransactionType().contentEquals(transactionType)
							&& !transaction.getTransactionSourceFile().contentEquals(transactionSourceFile)) {
						// duplicate, sell into two different accounts
						return;
					}
				}
			}
			transactionsForDate = new ArrayList<Transaction>();
			withdrawals.put(transactionDate, transactionsForDate);
		}
//		System.out.println(transactionDate + ": "
//				+ String.format("%-12s", CurrencyHelper.formatAsCurrencyString(transastionPrincipal)) + " "
//				+ String.format("%-10s", transactionType) + ": " + getShortName());

		transactionsForDate.add(newTransaction);

	}

	public void addExchange(LocalDate transactionDate, String transactionType, Float transactionShares,
			BigDecimal transactionSharePrice, BigDecimal principalAmount, String transactionSourceFile) {
		Transaction newTransaction = new Transaction(transactionDate, getSymbol(), transactionType, transactionShares,
				transactionSharePrice, principalAmount, transactionSourceFile);

		Collection<Transaction> transactionsForDate = exchanges.get(transactionDate);
		if (transactionsForDate != null) {
			for (Transaction transaction : transactionsForDate) {
				if (transaction.getTransactionType().contentEquals(transactionType)
						&& !transaction.getTransactionSourceFile().contentEquals(transactionSourceFile)) {
					// duplicate
					return;
				}
			}
		} else {
			transactionsForDate = exchanges.get(transactionDate.minusDays(1));
			if (transactionsForDate != null) {
				for (Transaction transaction : transactionsForDate) {
					if (transaction.getTransactionType().contentEquals(transactionType)
							&& !transaction.getTransactionSourceFile().contentEquals(transactionSourceFile)) {
						// duplicate
						return;
					}
				}
			}
			transactionsForDate = new ArrayList<Transaction>();
			exchanges.put(transactionDate, transactionsForDate);
		}
//		System.out.println(
//				transactionDate + ": " + String.format("%-12s", CurrencyHelper.formatAsCurrencyString(principalAmount))
//						+ " " + String.format("%-10s", transactionType) + ": " + getShortName());

		transactionsForDate.add(newTransaction);
	}

	public void addShareConversion(LocalDate transactionDate, String transactionType, Float transactionShares,
			BigDecimal transactionSharePrice, BigDecimal transastionPrincipal, String transactionSourceFile) {

		this.oldFundConverted = transactionDate;
		// outgoing transaction but this is easier to check
		if (getShares() == 0) {
			isClosed = true;			
		}
		
		Transaction newTransaction = new Transaction(transactionDate, getSymbol(), transactionType, transactionShares,
				transactionSharePrice, transastionPrincipal, transactionSourceFile);

		Collection<Transaction> transactionsForDate = shareConversions.get(transactionDate);
		if (transactionsForDate != null) {
			for (Transaction transaction : transactionsForDate) {
				if (transaction.getTransactionType().contentEquals(transactionType)) {
					// duplicate
					return;
				}
			}
		} else {
			transactionsForDate = shareConversions.get(transactionDate.minusDays(1));
			if (transactionsForDate != null) {
				for (Transaction transaction : transactionsForDate) {
					if (transaction.getTransactionType().contentEquals(transactionType)) {
						// duplicate
						return;
					}
				}
			}
			transactionsForDate = new ArrayList<Transaction>();
			shareConversions.put(transactionDate, transactionsForDate);
		}
//		System.out.println(transactionDate + ": "
//				+ String.format("%-12s", CurrencyHelper.formatAsCurrencyString(transastionPrincipal)) + " "
//				+ String.format("%-10s", transactionType) + ": " + getShortName());

		transactionsForDate.add(newTransaction);
	}

	public BigDecimal getConversionsUpToDate(LocalDate historicalDate) {
		BigDecimal conversionAmount = new BigDecimal(0);

		for (Entry<LocalDate, Collection<Transaction>> entry : shareConversions.entrySet()) {
			if (!entry.getKey().isBefore(historicalDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					conversionAmount = conversionAmount.add(transaction.getTransastionPrincipal());
				}
			}
		}
		return conversionAmount;
	}
	public Float getConversionsSharesUpToDate(LocalDate historicalDate) {
		Float conversionSharesAmount = 0f;

		for (Entry<LocalDate, Collection<Transaction>> entry : shareConversions.entrySet()) {
			if (!entry.getKey().isBefore(historicalDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					conversionSharesAmount = conversionSharesAmount + transaction.getTransastionShares();
				}
			}
		}
		return conversionSharesAmount;
	}

	public boolean isMMFund() {
		return getCategoriesMap().get(FundCategory.CASH).compareTo(BigDecimal.ZERO) != 0;
	}

	public List<Entry<LocalDate, Transaction>> getTransactionsBetweenDates(LocalDate startDate, LocalDate endDate) {
		List<Entry<LocalDate, Transaction>> transactions = new ArrayList<>();
		
		for (Entry<LocalDate, Collection<Transaction>> entry : income.entrySet()) {
			LocalDate transactionDate = entry.getKey();
			if (transactionDate.isBefore(endDate) && transactionDate.isAfter(startDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					Entry<LocalDate, Transaction> transactionEntry = Map.entry(transaction.getTransactionDate(), transaction);
					transactions.add(transactionEntry);
				}
			}
		}
		for (Entry<LocalDate, Collection<Transaction>> entry : withdrawals.entrySet()) {
			LocalDate transactionDate = entry.getKey();
			if (transactionDate.isBefore(endDate) && transactionDate.isAfter(startDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					Entry<LocalDate, Transaction> transactionEntry = Map.entry(transaction.getTransactionDate(), transaction);
					transactions.add(transactionEntry);
				}
			}
		}
		for (Entry<LocalDate, Collection<Transaction>> entry : exchanges.entrySet()) {
			LocalDate transactionDate = entry.getKey();
			if (transactionDate.isBefore(endDate) && transactionDate.isAfter(startDate)) {
				Collection<Transaction> transactionList = entry.getValue();
				for (Transaction transaction : transactionList) {
					Entry<LocalDate, Transaction> transactionEntry = Map.entry(transaction.getTransactionDate(), transaction);
					transactions.add(transactionEntry);
				}
			}
		}
		return transactions;
	}



}
