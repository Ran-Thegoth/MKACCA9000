package ir.esfandune.calculatorlibe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import rs.mkacca.R;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;


public abstract class CalculatorDialog implements View.OnClickListener {
    DecimalFormat df = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private Button mCurrentButton;
    private TextView history, Nowtext;
    private Context c;
    private ImageView img_done;
    private AlertDialog.Builder alertDialog;
    private View v;
    private boolean _isFrist;
    private final char  thousandSprt=',';
    private BigDecimal _result;

    public CalculatorDialog(Context context) {
        c = context;
        alertDialog = new AlertDialog.Builder(c);
        v = LayoutInflater.from(c).inflate(R.layout.alrt_calculator, new LinearLayout(context),false);
        alertDialog.setView(v);
        alertDialog.create();
        history = v.findViewById(R.id.history);
        Nowtext = v.findViewById(R.id.Nowtext);
        img_done = v.findViewById(R.id.done);

        Nowtext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Nowtext.removeTextChangedListener(this);
                try {
                    DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
                    DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
                    symbols.setGroupingSeparator(thousandSprt);
                    formatter.setDecimalFormatSymbols(symbols);

                    Nowtext.setText(formatter.format(Long.parseLong(charSequence.toString().replace(thousandSprt+"",""))));

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }finally {
                    Nowtext.addTextChangedListener(this);
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public static void easyCalculate(Activity c, final TextView et_price, boolean round) {
        easyCalculate(c, et_price, ",", false, round);
    }

    public static void easyCalculate(Activity c, final TextView et_price, String spliter, final boolean absRslt, final boolean round) {
        double value = 0;
        try {
            value = Double.parseDouble(et_price.getText().toString().trim().replaceAll(spliter, ""));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        new CalculatorDialog(c) {
            @Override
            public void onResult(BigDecimal value) {
//                NumberFormat nf = NumberFormat.getInstance();
                try {
                    double number = value.doubleValue();
                    if (round)
                        number = ((absRslt ? Math.abs(Math.round(number)) : Math.round(number)));
                    else
                        number = (absRslt ? Math.abs(number) : number);
                    et_price.setText(df.format(number));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.setValue(value).showDIalog();
    }

    public CalculatorDialog showDIalog() {
    	_isFrist = true;
//        EditText editText = (EditText)v.findViewById(R.id.label_field);
        final AlertDialog ad = alertDialog.show();
        v.findViewById(R.id.digit_0).setOnClickListener(this);
        v.findViewById(R.id.digit_00).setOnClickListener(this);
        v.findViewById(R.id.digit_000).setOnClickListener(this);
        v.findViewById(R.id.digit_1).setOnClickListener(this);
        v.findViewById(R.id.digit_2).setOnClickListener(this);
        v.findViewById(R.id.digit_3).setOnClickListener(this);
        v.findViewById(R.id.digit_4).setOnClickListener(this);
        v.findViewById(R.id.digit_5).setOnClickListener(this);
        v.findViewById(R.id.digit_6).setOnClickListener(this);
        v.findViewById(R.id.digit_7).setOnClickListener(this);
        v.findViewById(R.id.digit_8).setOnClickListener(this);
        v.findViewById(R.id.digit_9).setOnClickListener(this);
        v.findViewById(R.id.eq).setOnClickListener(this);
        v.findViewById(R.id.clr).setOnClickListener(this);
        v.findViewById(R.id.op_div).setOnClickListener(this);
        v.findViewById(R.id.op_mul).setOnClickListener(this);
        v.findViewById(R.id.op_sub).setOnClickListener(this);
        v.findViewById(R.id.op_add).setOnClickListener(this);
        v.findViewById(R.id.dot).setOnClickListener(this);
        v.findViewById(R.id.dlteAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ZeroCalculator();
            }
        });
        v.findViewById(R.id.clr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClearAnumber();
            }
        });
        img_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	recalc();
                onResult(_result);
                ad.dismiss();
            }
        });

        DisplayMetrics displayMetrics = c.getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        if (width < height)//istade
            ad.getWindow().setLayout((width * 90 / 100), (height * 70 / 100));
        else //khabide
            ad.getWindow().setLayout((width * 60 / 100), (height * 90 / 100));

        ad.getWindow().getDecorView().setBackgroundResource(R.drawable.bg_corner_for_dialogs);
        //////////////////////////////////////////
        return this;
    }

    public CalculatorDialog ClearAnumber() {
        String S_Nowtext = Nowtext.getText().toString();
        String S_history = history.getText().toString();
        if (TextUtils.isEmpty(Nowtext.getText()) || S_Nowtext.length() <= 1) {
            Nowtext.setText("0");
            if (S_history.length() > 1)
                history.setText(S_history.substring(0, S_history.length() - 1));
            else history.setText("0");
        } else {
            history.setText(S_history.substring(0, S_history.length() - 1));
            Nowtext.setText(S_Nowtext.substring(0, S_Nowtext.length() - 1));
        }
        return this;
    }

    public CalculatorDialog ZeroCalculator() {
        if (Nowtext != null && history != null) {
            Nowtext.setText("0");
            history.setText("0");
        }
        return this;
    }

    public String eval(final String str) {
        return df.format(new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `−` term
            // term = factor | term `×` factor | term `÷` factor
            // factor = `+` factor | `−` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('−') || eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('×')) x *= parseFactor(); // multiplication
                    else if (eat('÷')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('−') || eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse());
    }

    public abstract void onResult(BigDecimal value);

    private void recalc() {
        try {
        	String S_history = history.getText().toString();
            String natije;
            if (Character.isDigit(S_history.charAt(S_history.length() - 1)))
                natije = eval(S_history);
            else 
            	natije = eval(S_history.substring(0, S_history.length() - 1));
            _result = BigDecimal.valueOf(Double.parseDouble(natije.replace(thousandSprt, '.')));
            Nowtext.setText(natije);
            history.setText(natije);
            img_done.setColorFilter(null);
            img_done.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            _result = BigDecimal.ZERO;
            Toast.makeText(c, "معادله کامل نیست!", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onClick(View view) {
        img_done.setColorFilter(Color.parseColor("#80ffffff"), PorterDuff.Mode.SRC_IN);
        
        mCurrentButton = (Button) view;
        String S_Nowtext = Nowtext.getText().toString();
        String S_history = history.getText().toString();
        String S_btn = mCurrentButton.getText().toString();
        char lastChar = S_history.charAt(S_history.length() - 1);
        int ViewID = view.getId();
        try {
        if (ViewID == R.id.eq) {
        	recalc();
        } else if (ViewID == R.id.op_add || ViewID == R.id.op_sub || ViewID == R.id.op_mul || ViewID == R.id.op_div) {
            char perlastChar = (S_history.length() >= 2) ? S_history.charAt(S_history.length() - 2) : 0;
            if (Character.isDigit(lastChar)) {
                //اگر آخرین کاراکتر عدد هست
                Nowtext.setText("0");
                if (S_history.trim().equals("0") && S_btn.trim().equals(c.getString(R.string.op_sub)))
                    history.setText(S_btn);
                else
                    history.append(S_btn);
            } else {
                //اگر آخرین کاراکتر اپراتور هست
                if (S_btn.trim().equals(c.getString(R.string.op_sub))) {
                    //اگررو -یا+ زده و آخرین کاراکتر اپراتور منها هست،  باید فقط یکی منها چاپ بشه

                    if (lastChar != c.getString(R.string.op_sub).toCharArray()[0])
                        history.append(S_btn);
                } else {
                    //اگر رو منها نزده
                    if (!Character.isDigit(perlastChar))
                        history.setText((S_history.substring(0, S_history.length() - 2)) + S_btn);
                    else
                        history.setText((S_history.substring(0, S_history.length() - 1)) + S_btn);
                }
            }
        } else if (ViewID == R.id.dot) {
            if (Character.isDigit(lastChar) && !S_Nowtext.contains(".")) {
                history.append(".");
                Nowtext.append(".");
            }
        } else {
            if(_isFrist) {
            	Nowtext.setText(null);
            	history.setText(null);
            	_isFrist = false;
            }
            if (S_Nowtext.trim().equals("0"))
                Nowtext.setText(S_btn);
            else
                Nowtext.append(S_btn);
            /////
            if (S_history.trim().equals("0"))
                history.setText(S_btn);
            else
                history.append(S_btn);
        }
        } finally {
        }
    }

    public CalculatorDialog setValue(double input) {
        history.setText(df.format(input));
        Nowtext.setText(df.format(input));
        return this;

    }
}

