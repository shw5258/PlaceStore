package io.google.placestore;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class MyLocationRequestDialogFragment extends DialogFragment {
    
    private Dialog mDialog;
    
    public Dialog getmDialog() {
        return mDialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("기기의 위치정보를 사용하도록 허가하시겠습니까?");
        builder.setMessage("위치정보조회를 허가하시면 나의위치에서 지도를 시작하고 \"길찾기\"때 출발지로 자동등록됩니다.");
        builder.setPositiveButton("허가", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MapsActivity) getActivity()).requestPermReq();
            }
        });
        builder.setNegativeButton("보류", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    
        mDialog = builder.create();
        return mDialog;
    }
}
