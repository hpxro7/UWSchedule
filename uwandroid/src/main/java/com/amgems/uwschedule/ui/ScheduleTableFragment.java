package com.amgems.uwschedule.ui;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.amgems.uwschedule.R;
import com.amgems.uwschedule.model.Course;
import com.amgems.uwschedule.model.Meeting;
import com.amgems.uwschedule.model.PaddedCourseMeeting;
import com.amgems.uwschedule.model.Timetable;
import com.amgems.uwschedule.model.PaddedCourseMeeting;
import com.amgems.uwschedule.provider.ScheduleContract;
import com.etsy.android.grid.StaggeredGridView;
import com.etsy.android.grid.util.DynamicHeightTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class ScheduleTableFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<Cursor> {

    /** Courses cursor loader ID */
    private static final int COURSE_CURSOR_LOADER = 0;
    private static final int MEETINGS_CURSOR_LOADER = 1;

    private static final String BUNDLE_SLN_KEY = "bundleSln";

    private ArrayAdapter<PaddedCourseMeeting> mAdapter;
    private Map<String, Course> mCourseMap;
    private Timetable mTimetable;

    private Map<Course, Integer> mColorMap;

    static class ViewHolder {
        DynamicHeightTextView textView;
    }
    public ScheduleTableFragment() {
        // Required empty public constructor
    }

    public static ScheduleTableFragment newInstance() {
        return new ScheduleTableFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCourseMap = new TreeMap<String, Course>();
        getLoaderManager().initLoader(COURSE_CURSOR_LOADER, null, this);
        mColorMap = new HashMap<Course, Integer>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.schedule_table_fragment, container, false);
        StaggeredGridView scheduleTableView = (StaggeredGridView) rootView.findViewById(R.id
                .schedule_table);
        mAdapter = new ArrayAdapter<PaddedCourseMeeting>(getActivity(),
                R.layout.schedule_table_cell, R.id.schedule_table_cell) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder vh;
                if (convertView == null) {
                    convertView = View.inflate(getActivity(),R.layout.schedule_table_cell,
                            null);
                    vh = new ViewHolder();
                    vh.textView = (DynamicHeightTextView) convertView.findViewById(R.id
                            .schedule_table_cell);

                    convertView.setTag(vh);
                }
                else {
                    vh = (ViewHolder) convertView.getTag();
                }

                PaddedCourseMeeting event = mAdapter.getItem(position);
                vh.textView.setText("[" + position + "]" + event.toString());
                vh.textView.setHeight((event.getEndTime() - event.getStartTime()) * 2);
                vh.textView.setBackgroundColor(mColorMap.get(event.getCourseMeeting().getCourse()));
                vh.textView.setTextColor(Color.WHITE);

                return convertView;
            }
        };
        scheduleTableView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case COURSE_CURSOR_LOADER: {
                return new CursorLoader(getActivity(), ScheduleContract.Courses.CONTENT_URI, null, null, null, null);
            }
            case MEETINGS_CURSOR_LOADER: {
                ArrayList<String> slnList = args.getStringArrayList(BUNDLE_SLN_KEY);
                String[] selection = slnList.toArray(new String[slnList.size()]);
                return new CursorLoader(getActivity(), ScheduleContract.Meetings.CONTENT_URI, null,
                        buildSlnWhereClause(slnList.size()), selection, null);
            }
            default:
                throw new IllegalArgumentException("Illegal loader id requested: " + id);
        }


    }

    private String buildSlnWhereClause(int times) {
//        String result = ScheduleContract.Meetings.SLN + " = ?";
//        for (int i = 0; i < times - 1; i++) {
//            result +=  " OR " + ScheduleContract.Meetings.SLN + " = ?";
//        }
//        return result;
        String result = ScheduleContract.Meetings.SLN + " IN (?";
        for (int i = 1; i < times; i++) {
            result += ", ?";
        }
        return result + ")";
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Removes reference to old cursor adapter
        Log.d(ScheduleFragment.class.getSimpleName(), "Loader resetting");
    }

    private Course courseInstance(Cursor data) {
        String sln = data.getString(data.getColumnIndex(ScheduleContract.Courses.SLN));
        String departmentCode = data.getString(data.getColumnIndex(
                ScheduleContract.Courses.DEPARTMENT_CODE));
        int courseNumber = data.getInt(data.getColumnIndex(ScheduleContract.Courses.COURSE_NUMBER));
        String sectionId = data.getString(data.getColumnIndex(ScheduleContract.Courses.SECTION_ID));
        int credits = data.getInt(data.getColumnIndex(ScheduleContract.Courses.CREDITS));
        String title = data.getString(data.getColumnIndex(ScheduleContract.Courses.TITLE));
        Course.Type type = Course.Type.getType(data.getString(data.getColumnIndex(ScheduleContract
                .Courses.TYPE)));
        return new Course(sln, departmentCode, courseNumber, sectionId, credits, title, type,
                new ArrayList<Meeting>());
    }

    private Meeting meetingInstance(Cursor data) {
        Set<Meeting.Day> meetDays = new HashSet<Meeting.Day>();
        for (Meeting.Day day : Meeting.Day.values()) {
            int hasMeet = data.getInt(data.getColumnIndex(day.getColumnName()));
            if (hasMeet == ScheduleContract.Meetings.HAS_MEETING) {
                meetDays.add(day);
            }
        }
        String startTime = data.getString(data.getColumnIndex(ScheduleContract.Meetings
                .START_TIME));
        String endTime = data.getString(data.getColumnIndex(ScheduleContract.Meetings
                .END_TIME));
        String timespan = startTime + "-" + endTime;
        Meeting.Builder builder = new Meeting.Builder(meetDays, timespan);
        return builder.build();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case COURSE_CURSOR_LOADER: {
                List<String> slnList = new ArrayList<String>();
                if (data.getCount() > 0) {
                    data.moveToFirst();
                    while (!data.isAfterLast()) {
                        Course course = courseInstance(data);
                        mCourseMap.put(course.getSln(), course);
                        slnList.add(course.getSln());
                        data.moveToNext();
                    }
                    Bundle slnBundle = new Bundle();
                    slnBundle.putStringArrayList(BUNDLE_SLN_KEY, (ArrayList<String>) slnList);
                    if (getLoaderManager().getLoader(MEETINGS_CURSOR_LOADER) == null) {
                        getLoaderManager().initLoader(MEETINGS_CURSOR_LOADER, slnBundle, this);
                    } else {
                        getLoaderManager().restartLoader(MEETINGS_CURSOR_LOADER, slnBundle, this);
                    }
                }
                break;
            }
            case MEETINGS_CURSOR_LOADER: {
                if (data.getCount() > 0) {
                    data.moveToFirst();
                    while (!data.isAfterLast()) {
                        Meeting meeting = meetingInstance(data);
                        String sln = data.getString(data.getColumnIndex(ScheduleContract.Meetings.SLN));
                        mCourseMap.get(sln).getMeetings().add(meeting);
                        data.moveToNext();
                    }
                    mTimetable = new Timetable(new ArrayList<Course>(mCourseMap.values()));
                    mAdapter.clear();
                    mAdapter.addAll(mTimetable.toHorizontalList());
                    TypedArray colors = getResources().obtainTypedArray(R.array.colors);
                    int colorIndex = 0;
                    for (PaddedCourseMeeting meeting : mTimetable.toVerticalList()) {
                        Course c = meeting.getCourseMeeting().getCourse();
                        if (mColorMap.get(c) == null) {
                            mColorMap.put(c, colors.getColor(colorIndex, 0));
                        }
                        colorIndex++;
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unrecognized loader finished: " + loader.getId());
        }
    }
}