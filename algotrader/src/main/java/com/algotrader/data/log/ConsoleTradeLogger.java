// package com.algotrader.data.log;

// import com.algotrader.data.dataobjects.TradeExecutionLog;

// /**
//  * A simple {@link TradeLogger} implementation that prints trade execution logs
//  * to the console and tracks cumulative account balance change.
//  *
//  * <p>This logger is primarily intended for development, debugging, and simple
//  * paper-trading simulations.</p>
//  */
// public class ConsoleTradeLogger implements TradeLogger {

//     /**
//      * Running total of net account balance change across all logged executions.
//      */
//     private double totalNetBalanceChange = 0.0;

//     /**
//      * Prints the provided {@link TradeExecutionLog} to standard output and
//      * updates the cumulative balance change.
//      *
//      * @param log the trade execution log to print
//      * @throws IllegalArgumentException if {@code log} is null
//      */
//     @Override
//     public void log(TradeExecutionLog log) {
//         if (log == null) {
//             throw new IllegalArgumentException("TradeExecutionLog cannot be null.");
//         }

//         totalNetBalanceChange += log.getNetBalanceChange();

//         System.out.println(log);
//     }

//     /**
//      * Returns the cumulative net balance change across all logged executions.
//      *
//      * @return the total net balance change
//      */
//     public double getTotalNetBalanceChange() {
//         return totalNetBalanceChange;
//     }

//     /**
//      * Prints the cumulative net balance change to standard output.
//      */
//     public void printTotalNetBalanceChange() {
//         System.out.printf(
//                 "Total Net Balance Change: %.2f%n",
//                 totalNetBalanceChange
//         );
//     }
// }