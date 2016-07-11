package com.example.workhours

import com.vaadin.navigator.Navigator
import com.vaadin.server.VaadinRequest
import com.vaadin.ui.TabSheet
import com.vaadin.ui.UI
import com.vaadin.ui.Label
import com.example.workhours.converters.GleeoTimeTrackerConverterView;
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset;

@Theme('Workhours')
class WorkhoursUI extends UI{
	
	@Override
	void init(VaadinRequest request){
		content = new TabSheet()

		def navigator = new Navigator(this, content)
		navigator.addView('GleeoTimeTracker', GleeoTimeTrackerConverterView)
		
		navigator.setErrorView(GleeoTimeTrackerConverterView)
		
	}
}
