package com.minimalpay.settlement.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.minimalpay.settlement.MinimalPayApp;
import com.minimalpay.settlement.R;

/**
 * 위저드형 4단계: [이전] [다음]으로만 이동 (스와이프 비활성).
 */
public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView textStepIndicator;
    private MaterialButton btnPrevious;
    private MaterialButton btnNext;
    private SettlementSession session;

    private static final int[] STEP_TITLE_RES = {
            R.string.step_title_group,
            R.string.step_title_expense,
            R.string.step_title_report,
            R.string.step_title_transfer
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = MinimalPayApp.sessionFrom(getApplication());

        textStepIndicator = findViewById(R.id.textStepIndicator);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(4);
        viewPager.setUserInputEnabled(false);

        btnPrevious.setOnClickListener(v -> goPrevious());
        btnNext.setOnClickListener(v -> goNext());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateWizardChrome(position);
            }
        });

        updateWizardChrome(0);
    }

    private void goPrevious() {
        int current = viewPager.getCurrentItem();
        if (current > 0) {
            viewPager.setCurrentItem(current - 1, true);
        }
    }

    private void goNext() {
        int current = viewPager.getCurrentItem();

        if (current == StepValidator.STEP_TRANSFER) {
            Toast.makeText(this, R.string.wizard_complete, Toast.LENGTH_LONG).show();
            return;
        }

        StepValidator.ValidationResult result = StepValidator.validate(session, current);
        if (!result.isValid()) {
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        viewPager.setCurrentItem(current + 1, true);
    }

    /** Fragment에서 UI 갱신용 */
    public void refreshWizardState() {
        updateWizardChrome(viewPager.getCurrentItem());
    }

    private void updateWizardChrome(int step) {
        textStepIndicator.setText(STEP_TITLE_RES[step]);

        btnPrevious.setEnabled(step > StepValidator.STEP_GROUP);
        btnPrevious.setAlpha(step > StepValidator.STEP_GROUP ? 1f : 0.4f);

        if (step == StepValidator.STEP_TRANSFER) {
            btnNext.setText(R.string.btn_finish);
        } else {
            btnNext.setText(R.string.btn_next);
        }
    }
}
