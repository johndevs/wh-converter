package com.example.workhours.converters

import java.text.SimpleDateFormat;

import com.example.workhours.model.Model
import com.example.workhours.model.Task
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.vaadin.data.Item
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator
import com.vaadin.data.Container.Indexed
import com.vaadin.data.fieldgroup.FieldGroup
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.converter.StringToBigIntegerConverter
import com.vaadin.data.util.converter.StringToDoubleConverter
import com.vaadin.data.util.converter.StringToIntegerConverter
import com.vaadin.data.validator.NullValidator
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource
import com.vaadin.server.StreamResource.StreamSource
import com.vaadin.ui.Button
import com.vaadin.ui.ComboBox
import com.vaadin.ui.CustomComponent
import com.vaadin.ui.Field
import com.vaadin.ui.Grid
import com.vaadin.ui.HorizontalLayout
import com.vaadin.ui.Label
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout

import groovy.text.SimpleTemplateEngine

import com.vaadin.ui.Upload.SucceededEvent;

class GleeoTimeTrackerConverterView extends CustomComponent implements View {

	private File inputFile
	
	private final Table grid = new Table()
	
	GleeoTimeTrackerConverterView() {
		caption = 'Gleeo Time Tracker'
		
		def root = new VerticalLayout()
		compositionRoot = root
			
		def upload = new Upload(null, { String filename, String mimeType ->
			inputFile = File.createTempFile('gleeo-time-tracker-', filename)
			new FileOutputStream(inputFile)
		})
		upload.immediate = true
		root.addComponent new HorizontalLayout(new Label('CSV File'), upload)
		
		grid.setSizeFull()
		root.addComponent grid
		
		FieldGroup fields = new FieldGroup()
				
		def name = new ComboBox(
			inputPrompt: 'Project name',
			enabled:false,
			required: true,
			nullSelectionAllowed: false)
		fields.bind(name, name)
		
		def code = new ComboBox(
			inputPrompt: 'Project code', 
			enabled:false, 
			required: true, 
			nullSelectionAllowed: false)
		fields.bind(code, code)
		
		def task = new ComboBox(
			inputPrompt: 'Task name', 
			enabled:false, 
			required: true, 
			nullSelectionAllowed: false)
		fields.bind(task, task)
		
		def date = new ComboBox(
			inputPrompt: 'Date',
			enabled:false,
			required: true,
			nullSelectionAllowed: false)
		fields.bind(date, date)
		
		def duration = new ComboBox(
			inputPrompt: 'Duration',
			enabled:false,
			required: true,
			nullSelectionAllowed: false)
		fields.bind(duration, duration)
	
		def download = new Button(
			caption:'Export to WH',
			enabled: false)
		
		[name, code,task,date,duration].each { Field field ->
			// Bind all fields to fields group	
			fields.bind(field, field)
			field.addValidator(new NullValidator('Field cannot be null', false))
			field.addValueChangeListener({
				download.enabled = fields.isValid()
			} as ValueChangeListener)
		}
								
		root.addComponent new HorizontalLayout(name, code, task, date, duration, download)
		
		// Handle uploaded file
		upload.addSucceededListener { SucceededEvent event ->
			inputFile.withReader { Reader reader ->
				
				// Read column headers								
				def columnTypes = reader.readLine().split(';')
				
				[name, code, task, date, duration].each { 
					it.enabled = true
					it.removeAllItems()
					it.addItems(columnTypes)
				}
				
				// Defaults
				name.value = 'Project'
				code.value = 'Project-Extra-1'
				task.value = 'Task'
				date.value = 'Start'
				duration.value = 'Decimal Duration'
				
				// Read columns
				def entries = new IndexedContainer()								
				columnTypes.each { String type -> 
					entries.addContainerProperty(type, String, "")
				}
				
				// Read values
				reader.readLines().each { String line ->  
					def values = line.split(';')
					def itemId = entries.addItem()
					values.eachWithIndex { String value, Integer index ->
						def type = columnTypes[index]
						Item item = entries.getItem(itemId)
						item.getItemProperty(type).value = value
					}	
				}
				
				grid.containerDataSource = entries
				grid.pageLength = entries.size()
			}
		}
		
		// Handle file download
		new FileDownloader(new StreamResource({ 
			def data = grid.containerDataSource
			def model = new Model()
			data.itemIds.each { itemId ->
				Item item = data.getItem(itemId)
				
				// Add project code
				String taskProjectName = item.getItemProperty(name.value).value.toString().replaceAll("\\s","")
				String taskProjectCode = item.getItemProperty(code.value).value
				model.projects.put(taskProjectName, taskProjectCode)
								
				// Add task
				Date d = new SimpleDateFormat('yyyy-MM-dd H:m').parse(item.getItemProperty(date.value).value)
				def taskModel = new Task(					
					projectName: taskProjectName,
					description: item.getItemProperty(task.value).value,
					time: d,
					duration: Double.valueOf(item.getItemProperty(duration.value).value),
				)
			
				def day = new SimpleDateFormat('EE').format(d)		
				def group = "$day ${d[Calendar.DATE]}.${d[Calendar.MONTH]+1}."
				if(!model.tasks[group]){
					model.tasks[group] = []
				}
				model.tasks[group] << taskModel										
			}
			
			def file = getClass().classLoader.getResource('HenriFormat.template')
			def engine = new SimpleTemplateEngine(getClass().classLoader)
			def template = engine.createTemplate(file).make([model:model])
			new ByteArrayInputStream(template.toString().bytes)
						
		} as StreamSource , 'WH_EXPORT.txt')).extend(download)	
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		
	}

	

}
