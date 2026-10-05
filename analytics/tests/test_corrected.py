from pathlib import Path
import sys
import unittest
from unittest.mock import patch

import pandas as pd

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import analyze_corrected as corrected


class CorrectedAnalyticsTest(unittest.TestCase):
    def test_output_cannot_overwrite_legacy_or_run_artifacts(self):
        for path in [corrected.ROOT, corrected.ROOT/'reports/showcase',
                     corrected.ROOT/'reports/showcase'/corrected.LEGACY_ID,
                     corrected.ROOT/'reports/showcase'/corrected.LEGACY_ID/'nested',
                     corrected.ROOT/'data/logs/runs'/corrected.RUN_ID]:
            with self.subTest(path=path), self.assertRaises(ValueError):
                corrected.validate_output(path)
        self.assertEqual(corrected.validate_output(corrected.ROOT/'reports/showcase'/corrected.RUN_ID),
                         (corrected.ROOT/'reports/showcase'/corrected.RUN_ID).resolve())

    def test_trade_matching_uses_event_not_run_local_uuid(self):
        old=pd.DataFrame([dict(ticker='A',entry_time='t',trade_id='old',quantity=1)])
        new=pd.DataFrame([dict(ticker='A',entry_time='t',trade_id='new',quantity=2)])
        match=corrected.matched_trades(old,new)
        self.assertEqual(len(match),1)
        self.assertEqual(match.iloc[0].trade_id_corrected,'new')
        self.assertEqual(match.iloc[0].quantity_corrected,2)
        with self.assertRaises(ValueError):
            corrected.matched_trades(old,new.assign(entry_time='different'))
        with self.assertRaises(pd.errors.MergeError):
            corrected.matched_trades(old,pd.concat([new,new]))

    def test_direction_and_magnitude_use_distinct_terminal_labels(self):
        returns=[-.003,-.001,0,.001,.003,.004]
        frame=pd.DataFrame(dict(target_return=returns,probability=[.8,.2,.1,.3,.7,.9],
            volatility_return_units=[.003,.001,.0001,.001,.003,.004],
            realized_volatility=[.003,.001,.0001,.001,.003,.004],
            day=['day']*6,volatility_bucket=[0]*6,label=[0,0,0,0,1,1]))
        metrics={'prediction':{},'tables':{}}
        with patch.object(corrected.base,'auc_block_interval',return_value={'estimate':.5}):
            corrected.model_diagnostics(metrics,frame)
        result=metrics['prediction']['movement_diagnostics']
        self.assertEqual(result['positive_direction']['positive_count'],3)
        self.assertEqual(result['negative_threshold']['positive_count'],1)
        self.assertEqual(result['absolute_move']['positive_count'],3)
        self.assertEqual(result['direction_given_large_move']['n'],3)


if __name__=='__main__': unittest.main()
