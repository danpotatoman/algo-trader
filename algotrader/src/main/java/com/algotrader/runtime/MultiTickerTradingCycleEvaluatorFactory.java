package com.algotrader.runtime;

import com.algotrader.config.TradingSessionConfig;
import com.algotrader.marketdata.provider.DefaultMultiTickerWindowProvider;
import com.algotrader.marketdata.provider.MultiTickerWindowProvider;

public class MultiTickerTradingCycleEvaluatorFactory {
    public MultiTickerTradingCycleEvaluatorFactory() {

    }

    public MultiTickerTradingCycleEvaluator create(TradingSessionConfig sessionConfig) {
        MultiTickerWindowProvider windowProvider = new DefaultMultiTickerWindowProvider(null, null, 0, null);
        return new MultiTickerTradingCycleEvaluator(null, null, null, null);
    }
}