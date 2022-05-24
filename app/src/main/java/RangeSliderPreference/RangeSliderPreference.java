package RangeSliderPreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import sh.siava.AOSPMods.R;

public class RangeSliderPreference extends Preference {
    private final float valueFrom;
    private final float valueTo;
    private final float tickInterval;
    private final float defaultValue;
    RangeSlider slider;
    int valueCount;
    List<Float> prevValues = new ArrayList<>();

    boolean updateConstantly;

    public RangeSliderPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public RangeSliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a =context.obtainStyledAttributes(attrs, R.styleable.RangeSliderPreference);
        updateConstantly = a.getBoolean(R.styleable.RangeSliderPreference_updatesContinuously, false);
        valueCount = a.getInteger(R.styleable.RangeSliderPreference_valueCount, 1);
        valueFrom = a.getFloat(R.styleable.RangeSliderPreference_minVal, 0f);
        valueTo = a.getFloat(R.styleable.RangeSliderPreference_maxVal, 100f);
        tickInterval = a.getFloat(R.styleable.RangeSliderPreference_tickInterval, 1f);
        defaultValue = a.getFloat(R.styleable.RangeSliderPreference_defaultVal, valueFrom);
        a.recycle();

        setWidgetLayoutResource(R.layout.range_slider);
    }

    public void savePrefs()
    {
        List<Float> values = slider.getValues();
        Set<String> preSet = new ArraySet<>();
        for(float value : values)
        {
            preSet.add(String.valueOf(value));
        }
        getSharedPreferences().edit().putStringSet(getKey(), preSet).apply();
    }

    private void syncState() {
        Set<String> currentStrings = getSharedPreferences().getStringSet(getKey(), new ArraySet<>());
        List<Float> values = new ArrayList<>();
        currentStrings.iterator().forEachRemaining(v -> values.add(Float.valueOf(v)));
        for(float v : values)
        {
            if(v < slider.getValueFrom() || v > slider.getValueTo() || v%slider.getStepSize() != 0)
            {
                values.remove(v);
            }
        }
        if(values.size() == 0)
        {
            values.add(defaultValue);
        }

        if(valueCount > 1 && values.size() == 1)
        {
            values.add(values.get(0));
        }

        try {
            slider.setValues(values);
        } catch (Throwable t)
        {
            values.clear();
        }
        prevValues = new ArrayList<>();
        prevValues.addAll(slider.getValues());
    }

    RangeSlider.OnChangeListener changeListener = (slider, value, fromUser) -> {
        if(fromUser) resizeSliderValues();

        if(updateConstantly && fromUser)
        {
            savePrefs();
        }
    };

    private void resizeSliderValues()
    {
        if(slider.getValues().size() < valueCount)
        {
            slider.getValues().forEach(v -> {
                try {
                    prevValues.remove(v);
                }catch (Exception ignored){}
            });

            List<Float> v = slider.getValues();
            v.add(prevValues.get(0));
            slider.setValues(v);
        }
        prevValues.clear();
        prevValues.addAll(slider.getValues());
    }

    RangeSlider.OnSliderTouchListener sliderTouchListener = new RangeSlider.OnSliderTouchListener() {
        @Override
        public void onStartTrackingTouch(@NonNull RangeSlider slider) {}

        @Override
        public void onStopTrackingTouch(@NonNull RangeSlider slider) {
            if(!updateConstantly)
            {
                savePrefs();
            }
        }
    };

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        slider = (RangeSlider) holder.findViewById(R.id.range_slider_view);

        slider.setValueFrom(valueFrom);
        slider.setValueTo(valueTo);
        slider.setStepSize(tickInterval);

        syncState();

        slider.addOnSliderTouchListener(sliderTouchListener);
        slider.addOnChangeListener(changeListener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,400);
        holder.itemView.setLayoutParams(lp);
    }

    public static List<Float> getValues(SharedPreferences prefs, String Key, float defaultValue)
    {
        List<Float> result = new ArrayList<>();
        Set<String> strings = prefs.getStringSet(Key, new ArraySet<>());
        strings.forEach(s -> result.add(Float.valueOf(s)));
        if(result.size() == 0)
        {
            result.add(defaultValue);
        }
        return result;
    }
}