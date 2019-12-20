package com.loopeer.test.cardstackview;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.loopeer.cardstack.CardStackView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements CardStackView.ItemExpendListener {
    public static Integer[] TEST_DATAS = new Integer[]{
            R.color.color_1,
            R.color.color_2
            ,
            R.color.color_3
//            ,
//            R.color.color_4,
//            R.color.color_5,
//            R.color.color_6
//            ,
//            R.color.color_7,
//            R.color.color_8,
//            R.color.color_9,
//            R.color.color_10,
//            R.color.color_11,
//            R.color.color_12,
//            R.color.color_13,
//            R.color.color_14,
//            R.color.color_15,
//            R.color.color_16,
//            R.color.color_17,
//            R.color.color_18,
//            R.color.color_19,
//            R.color.color_20,
//            R.color.color_21,
//            R.color.color_22,
//            R.color.color_23,
//            R.color.color_24,
//            R.color.color_25,
//            R.color.color_26
    };
    private CardStackView mStackView;
    private TestStackAdapter mTestStackAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStackView = (CardStackView) findViewById(R.id.stackview_main);
        mStackView.setItemExpendListener(this);
        mTestStackAdapter = new TestStackAdapter(this);
        mStackView.setAdapter(mTestStackAdapter);
        TextView textView = new TextView(this);

        textView.setText("我是顶部");

        final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(lp);
        mStackView.setmHeadView(textView);


        TextView textView1 = new TextView(this);
        final ViewGroup.LayoutParams lp1 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        textView1.setText("我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部" +
                "我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部" +
                "我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部我是底部");
        textView1.setTextSize(20);
        textView1.setPadding(20,0,20,0);
        textView1.setLayoutParams(lp1);

        textView1.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.color_20));
        mStackView.setmBottomView(textView1);

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mTestStackAdapter.updateData(Arrays.asList(TEST_DATAS));
                    }
                }
                , 200
        );

    }





    @Override
    public void onItemExpend(boolean expend) {
//        Toast.makeText(getApplicationContext(),"打开状态"+expend,Toast.LENGTH_SHORT).show();
    }
}

