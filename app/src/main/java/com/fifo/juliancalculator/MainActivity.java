package com.fifo.juliancalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final ZoneId AUSTIN_ZONE = ZoneId.of("America/Chicago");
    private static final int PRIMARY = Color.rgb(103, 80, 164);
    private static final int SECONDARY = Color.rgb(232, 222, 248);
    private static final int SURFACE = Color.rgb(255, 251, 254);
    private static final int ON_SURFACE = Color.rgb(28, 27, 31);
    private static final int MUTED = Color.rgb(95, 91, 99);
    private static final int ERROR = Color.rgb(186, 26, 26);

    private EditText entry;
    private TextView modeTitle;
    private TextView inputLabel;
    private TextView resultLabel;
    private TextView result;
    private TextView error;
    private TextView today;
    private FrameLayout workSpace;
    private boolean reverse = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        updateTodayHeader();
        updateMode();
        entry.requestFocus();
        entry.postDelayed(new Runnable() {
            @Override public void run() {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(entry, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 250);
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(10), dp(22), dp(24));
        root.setBackgroundColor(SURFACE);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int topInset;
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    topInset = bars.top;
                    bottomInset = bars.bottom;
                } else {
                    topInset = insets.getSystemWindowInsetTop();
                    bottomInset = insets.getSystemWindowInsetBottom();
                }
                view.setPadding(dp(22), dp(10) + topInset,
                        dp(22), dp(24) + bottomInset);
                if (workSpace != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    int keyboardInset = insets.getInsets(WindowInsets.Type.ime()).bottom;
                    workSpace.setPadding(0, 0, 0, keyboardInset);
                }
                return insets;
            }
        });

        today = text("", 12, MUTED);
        today.setId(View.generateViewId());
        today.setTag("today");
        root.addView(today);

        workSpace = new FrameLayout(this);
        root.addView(workSpace, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout workArea = new LinearLayout(this);
        workArea.setOrientation(LinearLayout.VERTICAL);
        workArea.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams workAreaParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        workSpace.addView(workArea, workAreaParams);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(10), 0, dp(12));
        modeTitle = text("", 21, ON_SURFACE);
        modeTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(modeTitle, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button help = button("?", 22);
        help.setContentDescription("도움말");
        help.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showHelp(); }
        });
        header.addView(help, new LinearLayout.LayoutParams(dp(52), dp(52)));
        workArea.addView(header);

        inputLabel = text("", 14, MUTED);
        workArea.addView(inputLabel);
        entry = new EditText(this);
        entry.setTextSize(22);
        entry.setTextColor(ON_SURFACE);
        entry.setSingleLine(true);
        entry.setPadding(dp(16), dp(10), dp(16), dp(10));
        workArea.addView(entry, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)));
        entry.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                evaluate(editable.toString());
            }
        });

        error = text("", 13, ERROR);
        error.setMinHeight(dp(30));
        workArea.addView(error);

        resultLabel = text("", 14, MUTED);
        resultLabel.setGravity(Gravity.CENTER);
        resultLabel.setPadding(0, dp(22), 0, 0);
        workArea.addView(resultLabel);
        result = text("—", 56, ON_SURFACE);
        result.setGravity(Gravity.CENTER);
        result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        workArea.addView(result, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(94)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(16), 0, 0);
        Button flip = button("⇄", 26);
        flip.setContentDescription("변환 방향 바꾸기");
        flip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                reverse = !reverse;
                entry.setText("");
                updateMode();
                entry.requestFocus();
            }
        });
        Button clear = button("×", 26);
        clear.setContentDescription("입력 지우기");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                entry.setText("");
                entry.requestFocus();
            }
        });
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(112), dp(52));
        actionParams.setMargins(dp(7), 0, dp(7), 0);
        actions.addView(flip, actionParams);
        actions.addView(clear, actionParams);
        workArea.addView(actions);

        return root;
    }

    private void updateTodayHeader() {
        ZonedDateTime now = ZonedDateTime.now(AUSTIN_ZONE);
        String date = now.format(DateTimeFormatter.ofPattern("dd-MMM-uuuu", Locale.US));
        String julian = String.format(Locale.US, "%02d%03d", now.getYear() % 100, now.getDayOfYear());
        today.setText("Today's date is " + date + "\nToday's Julian Date is " + julian);
    }

    private void updateMode() {
        modeTitle.setText(reverse ? "날짜 → 줄리안 번호" : "줄리안 번호 → 날짜");
        inputLabel.setText(reverse ? "날짜" : "줄리안 번호 또는 YYDDD 코드");
        resultLabel.setText(reverse ? "줄리안 번호" : "FIFO 라벨 날짜");
        // Both workflows are number-first: 258 and 915 are the quickest normal inputs.
        // Separators remain accepted when pasted or entered from a hardware keyboard.
        entry.setInputType(InputType.TYPE_CLASS_NUMBER);
        error.setText("");
        result.setText("—");
    }

    private void evaluate(String raw) {
        error.setText("");
        result.setText("—");
        String value = raw.trim();
        if (value.isEmpty()) return;
        if (reverse) evaluateDate(value); else evaluateJulian(value);
    }

    private void evaluateJulian(String value) {
        if (!value.matches("\\d{3}") && !value.matches("\\d{5}")) return;
        int targetYear = value.length() == 5 ? 2000 + Integer.parseInt(value.substring(0, 2)) : ZonedDateTime.now(AUSTIN_ZONE).getYear();
        int ordinal = Integer.parseInt(value.substring(value.length() - 3));
        int maximum = LocalDate.of(targetYear, 12, 31).getDayOfYear();
        if (ordinal < 1 || ordinal > maximum) {
            error.setText("001~" + maximum + " 사이의 번호를 입력하세요.");
            return;
        }
        LocalDate date = LocalDate.ofYearDay(targetYear, ordinal);
        result.setText(date.getMonthValue() + "/" + String.format(Locale.US, "%02d", date.getDayOfMonth()));
    }

    private void evaluateDate(String value) {
        Integer month = null;
        Integer day = null;
        Matcher separated = Pattern.compile("^(\\d{1,2})\\s*[/\\-\\s]\\s*(\\d{1,2})$").matcher(value);
        if (separated.matches()) {
            month = Integer.parseInt(separated.group(1));
            day = Integer.parseInt(separated.group(2));
        } else if (value.matches("\\d{3}")) {
            month = Integer.parseInt(value.substring(0, 1));
            day = Integer.parseInt(value.substring(1));
        } else if (value.matches("\\d{4}")) {
            month = Integer.parseInt(value.substring(0, 2));
            day = Integer.parseInt(value.substring(2));
        } else if (value.length() >= 3) {
            error.setText("M DD, M/DD, MDD 또는 MMDD로 입력하세요.");
            return;
        } else return;
        try {
            LocalDate date = LocalDate.of(ZonedDateTime.now(AUSTIN_ZONE).getYear(), month, day);
            result.setText(String.format(Locale.US, "%03d", date.getDayOfYear()));
        } catch (DateTimeException ex) {
            error.setText("올바른 월/일을 입력하세요.");
        }
    }

    private void showHelp() {
        String message = reverse
                ? "날짜 → 줄리안 번호\n\n9 15, 9/15, 915 → 258\n11월 2일은 11 2 또는 1102로 입력합니다.\n\n세 자리 숫자는 MDD 형식입니다.\n112 = 1월 12일"
                : "줄리안 번호 → 날짜\n\n250 → 9/07\n26254 → 9/11\n\n5자리 코드는 YYDDD 형식입니다.\n⇄를 누르면 반대 변환을 합니다.";
        new AlertDialog.Builder(this)
                .setTitle("입력 방법")
                .setMessage(message)
                .setPositiveButton("닫기", null)
                .show();
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(String label, int sp) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextSize(sp);
        view.setTextColor(ON_SURFACE);
        view.setBackgroundColor(SECONDARY);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
