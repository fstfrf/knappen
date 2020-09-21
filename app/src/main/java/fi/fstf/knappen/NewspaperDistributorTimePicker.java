/*
 * Copyright 2020 Finlands svenska taltidningsförening rf.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *     */
package fi.fstf.knappen;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

// This class uses the built in timepicker from Android
public class NewspaperDistributorTimePicker extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newspaper_distributor_time_picker);

        final TimePicker alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        alarmTimePicker.setIs24HourView(true);

        Button cancelButton = (Button)findViewById(R.id.cancelTimePicker);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Return the cancel because the parent activity expects return value
                Intent output = new Intent();
                setResult(RESULT_CANCELED, output);
                finish();
            }
        });

        Button okTimePickerButton = (Button)findViewById(R.id.okTimePicker);
        okTimePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Return values as a result to NewspaperDistributorEdit class
                Intent output = new Intent();
                output.putExtra("FSTF_TIME_PICKER_HOUR", alarmTimePicker.getHour());
                output.putExtra("FSTF_TIME_PICKER_MINUTE", alarmTimePicker.getMinute());
                setResult(RESULT_OK, output);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
