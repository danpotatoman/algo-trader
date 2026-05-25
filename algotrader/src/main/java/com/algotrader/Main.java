package com.algotrader;

import java.util.List;

import com.algotrader.data.buffer.CSVDataBuffer;
import com.algotrader.data.buffer.DataBufferException;
import com.algotrader.data.log.ConsoleTradeLogger;
import com.algotrader.data.trader.CSVPaperTrader;
import com.algotrader.data.trader.TradeExecutor;
import com.algotrader.modelapi.PythonAPICaller;
import com.algotrader.prediction.interpretation.PredictionInterpreter;
import com.algotrader.prediction.interpretation.SimpleClassificationPredictionInterpreter;
import com.algotrader.service.AlgoTraderService;

public class Main {
    public static void main(String[] args) throws DataBufferException {

        CSVDataBuffer dataBuffer = new CSVDataBuffer("data");

        PythonAPICaller pythonAPICaller = new PythonAPICaller(
                "http://localhost:8000/predict"
        );

        double minimumConfidence = 0.55;
        PredictionInterpreter predictionInterpreter = new SimpleClassificationPredictionInterpreter(minimumConfidence);

        TradeExecutor tradeExecutor = new CSVPaperTrader(dataBuffer);

        ConsoleTradeLogger tradeLogger = new ConsoleTradeLogger();

        AlgoTraderService service = new AlgoTraderService(
                dataBuffer,
                pythonAPICaller,
                predictionInterpreter,
                tradeExecutor,
                tradeLogger,
                List.of("AAPL")
        );

        for(int i = 0; i < 20; i++) {
            service.runTradingCycle();
            service.runTradingCycle();
            service.runTradingCycle();
            service.runTradingCycle();
            service.runTradingCycle();
            tradeLogger.printTotalNetBalanceChange();
        }
        
    }
}