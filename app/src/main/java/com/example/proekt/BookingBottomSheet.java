package com.example.proekt;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class BookingBottomSheet extends BottomSheetDialogFragment {

    private String selectedTariff = "Эконом";

    private LatLng startLatLng;
    private LatLng endLatLng;

    private String startAddress = "";
    private String endAddress = "";

    public BookingBottomSheet() {
    }

    public static BookingBottomSheet newInstance(
            String startAddress,
            String endAddress,
            LatLng startLatLng,
            LatLng endLatLng
    ) {
        BookingBottomSheet sheet = new BookingBottomSheet();
        Bundle args = new Bundle();

        args.putString("startAddress", startAddress == null ? "" : startAddress);
        args.putString("endAddress", endAddress == null ? "" : endAddress);

        if (startLatLng != null) {
            args.putDouble("startLatitude", startLatLng.latitude);
            args.putDouble("startLongitude", startLatLng.longitude);
        }

        if (endLatLng != null) {
            args.putDouble("endLatitude", endLatLng.latitude);
            args.putDouble("endLongitude", endLatLng.longitude);
        }

        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setDimAmount(0.5f);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );

            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        restoreArguments();
        return setupBookingView(inflater, container);
    }

    private void restoreArguments() {
        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        startAddress = args.getString("startAddress", "");
        endAddress = args.getString("endAddress", "");

        if (args.containsKey("startLatitude") && args.containsKey("startLongitude")) {
            startLatLng = new LatLng(
                    args.getDouble("startLatitude"),
                    args.getDouble("startLongitude")
            );
        }

        if (args.containsKey("endLatitude") && args.containsKey("endLongitude")) {
            endLatLng = new LatLng(
                    args.getDouble("endLatitude"),
                    args.getDouble("endLongitude")
            );
        }
    }

    private View setupBookingView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(
                R.layout.bottom_sheet_booking,
                container,
                false
        );

        ImageButton closeButton = view.findViewById(R.id.closeButton);
        EditText fromAddress = view.findViewById(R.id.fromAddress);
        EditText toAddress = view.findViewById(R.id.toAddress);

        Button paymentToggle = view.findViewById(R.id.paymentToggle);
        Button orderButton = view.findViewById(R.id.orderButton);

        MaterialButton tariffEconom = view.findViewById(R.id.tariffEconom);
        MaterialButton tariffExpress = view.findViewById(R.id.tariffExpress);
        MaterialButton tariffLux = view.findViewById(R.id.tariffLux);

        closeButton.setOnClickListener(v -> dismiss());

        fromAddress.setText(getAddress(startLatLng));
        toAddress.setText(getAddress(endLatLng));

        View.OnClickListener tariffListener = v -> {
            tariffEconom.setStrokeWidth(0);
            tariffExpress.setStrokeWidth(0);
            tariffLux.setStrokeWidth(0);

            MaterialButton selected = (MaterialButton) v;
            selected.setStrokeWidth(4);
            selectedTariff = selected.getText().toString();
        };

        tariffEconom.setOnClickListener(tariffListener);
        tariffExpress.setOnClickListener(tariffListener);
        tariffLux.setOnClickListener(tariffListener);

        paymentToggle.setOnClickListener(v -> {
            String text = paymentToggle.getText().toString();

            if (text.contains("💵")) {
                paymentToggle.setText("💳 Карта");
            } else {
                paymentToggle.setText("💵 Наличные");
            }

            Toast.makeText(
                    getContext(),
                    "Способ оплаты: " + paymentToggle.getText(),
                    Toast.LENGTH_SHORT
            ).show();
        });

        orderButton.setOnClickListener(v -> {
            if (selectedTariff == null || selectedTariff.isEmpty()) {
                Toast.makeText(
                        getContext(),
                        "Пожалуйста, выберите тариф",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            startAddress = fromAddress.getText().toString();
            endAddress = toAddress.getText().toString();

            Toast.makeText(
                    getContext(),
                    "Выбран тариф: " + selectedTariff,
                    Toast.LENGTH_SHORT
            ).show();

            switchToWaitingScreen();
        });

        return view;
    }

    private String getAddress(LatLng latLng) {
        if (latLng == null) {
            return "";
        }

        Geocoder geocoder = new Geocoder(
                requireContext(),
                Locale.getDefault()
        );

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String result = address.getAddressLine(0);

                if (result != null) {
                    result = result.replace(", Россия", "");
                    result = result.replaceAll("\\b\\d{6}\\b", "");
                    result = result.replaceAll(",\\s*,", ",");
                    result = result.replaceAll(",\\s*$", "").trim();

                    return result;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return latLng.latitude + ", " + latLng.longitude;
    }

    private void switchToWaitingScreen() {
        ViewGroup root = requireView().findViewById(R.id.bookingRoot);

        root.removeAllViews();

        LayoutInflater.from(getContext()).inflate(
                R.layout.waiting_for_driver_layout,
                root,
                true
        );

        Button cancelButton = root.findViewById(R.id.cancelWaiting);

        cancelButton.setOnClickListener(v -> {
            Toast.makeText(
                    getContext(),
                    "Заказ отменён",
                    Toast.LENGTH_SHORT
            ).show();

            root.removeAllViews();

            View restored = setupBookingView(
                    LayoutInflater.from(getContext()),
                    root
            );

            root.addView(restored);
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();

        if (view == null || !(view.getParent() instanceof View)) {
            return;
        }

        View parent = (View) view.getParent();

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

        behavior.setPeekHeight(600);
        behavior.setHideable(true);

        parent.setBackgroundColor(Color.TRANSPARENT);
    }
}
