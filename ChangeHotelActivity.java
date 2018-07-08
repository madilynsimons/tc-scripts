package com.simons.owner.traffickcam2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ChangeHotelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_hotel);

        final Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);

        final ListView hotelList = (ListView) findViewById(R.id.HotelList);
        ArrayList<String> hotels = getIntent().getStringArrayListExtra("hotels");

        ListAdapter adapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, hotels);
        hotelList.setAdapter(adapter);


        hotelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String s = hotelList.getItemAtPosition(position).toString();
                returnIntent.putExtra("hotelresult", s);
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });


    }

}
