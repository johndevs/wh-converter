package com.example.workhours.model

import groovy.transform.Canonical

class Model {

	def Map<String, String> projects = [:]
	
	def Map<String, Task> tasks = [:]
	
}

@Canonical 
class Task {
	
	def String projectName
		
	def String description
	
	def Date time
	
	def Number duration
}