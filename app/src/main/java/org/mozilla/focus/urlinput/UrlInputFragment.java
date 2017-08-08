/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.widget.FlowLayout;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.InlineAutocompleteEditText;

import java.util.List;

/**
 * Fragment for displaying he URL input controls.
 */
public class UrlInputFragment extends Fragment implements UrlInputContract.View,
        View.OnClickListener, View.OnLongClickListener,
        InlineAutocompleteEditText.OnCommitListener {

    public static final String FRAGMENT_TAG = "url_input";

    private static final String ARGUMENT_URL = "url";

    private UrlInputContract.Presenter presenter;

    /**
     * Create a new UrlInputFragment and animate the url input view from the position/size of the
     * fake url bar view.
     */
    public static UrlInputFragment create(@Nullable String url) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);

        UrlInputFragment fragment = new UrlInputFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private InlineAutocompleteEditText urlView;
    private FlowLayout suggestionView;
    private View clearView;

    private UrlAutoCompleteFilter urlAutoCompleteFilter;
    private View dismissView;
    private TextChangeListener textChangeListener = new TextChangeListener();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.presenter = new UrlInputPresenter(getActivity());
        this.presenter.setView(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_urlinput, container, false);

        dismissView = view.findViewById(R.id.dismiss);
        dismissView.setOnClickListener(this);

        clearView = view.findViewById(R.id.clear);
        clearView.setOnClickListener(this);

        urlAutoCompleteFilter = new UrlAutoCompleteFilter();
        urlAutoCompleteFilter.loadDomainsInBackground(getContext().getApplicationContext());

        suggestionView = (FlowLayout) view.findViewById(R.id.search_suggestion);

        urlView = (InlineAutocompleteEditText) view.findViewById(R.id.url_edit);
        urlView.addTextChangedListener(textChangeListener);
        urlView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Avoid showing keyboard again when returning to the previous page by back key.
                if (hasFocus) {
                    ViewUtils.showKeyboard(urlView);
                } else {
                    ViewUtils.hideKeyboard(urlView);
                }
            }
        });

        urlView.setOnCommitListener(this);

        if (getArguments().containsKey(ARGUMENT_URL)) {
            urlView.setText(getArguments().getString(ARGUMENT_URL));
            clearView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        urlView.requestFocus();

        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STARTED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.setView(null);
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STOPPED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.suggestion_item:
                setUrlText(((TextView) view).getText());
                return true;
            case R.id.clear:
            case R.id.dismiss:
            default:
                return false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clear:
                urlView.setText("");
                urlView.requestFocus();
                break;
            case R.id.dismiss:
                dismiss();
                break;
            case R.id.suggestion_item:
                onSuggestionClicked(((TextView) view).getText());
                break;
            default:
                throw new IllegalStateException("Unhandled view in onClick()");
        }
    }

    private void onSuggestionClicked(CharSequence tag) {
        setUrlText(tag);
        onCommit();
    }

    private void dismiss() {
        // This method is called from animation callbacks. In the short time frame between the animation
        // starting and ending the activity can be paused. In this case this code can throw an
        // IllegalStateException because we already saved the state (of the activity / fragment) before
        // this transaction is committed. To avoid this we commit while allowing a state loss here.
        // We do not save any state in this fragment (It's getting destroyed) so this should not be a problem.
        final Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, FragmentListener.TYPE.DISMISS_URL_INPUT, true);
        }
    }

    public void onCommit() {
        final String input = urlView.getText().toString();
        if (!input.trim().isEmpty()) {
            ViewUtils.hideKeyboard(urlView);

            final boolean isUrl = UrlUtils.isUrl(input);

            final String url = isUrl
                    ? UrlUtils.normalize(input)
                    : UrlUtils.createSearchUrl(getContext(), input);

            openUrl(url);

            TelemetryWrapper.urlBarEvent(isUrl);
        }
    }

    private void openUrl(String url) {
        final Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, FragmentListener.TYPE.OPEN_URL, url);
        }
    }

    @Override
    public void setUrlText(CharSequence text) {
        this.urlView.removeTextChangedListener(textChangeListener);
        this.urlView.setText(text);
        this.urlView.setSelection(text.length());
        this.urlView.addTextChangedListener(textChangeListener);
    }

    @Override
    public void setSuggestions(@Nullable List<CharSequence> texts) {
        this.suggestionView.removeAllViews();
        if (texts == null) {
            return;
        }

        for (int i = 0; i < texts.size(); i++) {
            final TextView item = (TextView) View.inflate(getContext(), R.layout.tag_text, null);
            item.setText(texts.get(i));
            item.setOnClickListener(this);
            item.setOnLongClickListener(this);
            this.suggestionView.addView(item);

        }
    }

    private class TextChangeListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            UrlInputFragment.this.presenter.onInput(s);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}