package com.crystalvision;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private final static int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter adapter;
    private TextView textView;
    private TextView textAngle;
    private SeekBar seekAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = BluetoothAdapter.getDefaultAdapter();



        Button btnCheckBT = (Button) findViewById(R.id.check_bluetooth);
        textView = (TextView) findViewById(R.id.textView);
        textAngle = (TextView) findViewById(R.id.angle);
        seekAngle = (SeekBar) findViewById(R.id.setAngle);




        btnCheckBT.setOnClickListener(this);

        seekAngle.setOnSeekBarChangeListener(this);

    }

    private void checkBT(){
        if (adapter != null){
            if (adapter.isEnabled()){
                String address = adapter.getAddress();
                String name = adapter.getName();
                int state = adapter.getState();

                textView.setText(address + " " + name);

            //    Toast.makeText(this, state, Toast.LENGTH_SHORT).show();

            }else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.check_bluetooth:
                checkBT();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        textAngle.setText(progress+"");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
