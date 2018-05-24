/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.tasks

import java.util.HashMap
import java.util.regex.Pattern
import org.eclipse.xtext.util.Strings
import java.util.Map

/**
 * @author Stefan Oehme - Initial contribution and API
 * @since 2.6
 */
class DefaultTaskParser implements ITaskParser {

	override parseTasks(String source, TaskTags taskTags) {
		if (taskTags.empty) return #[]

		val taskTagsByName = getTaskTagsByName(taskTags)

		val matcher = taskTags.toPattern.matcher(source)
		val tasks = newArrayList
		// keep track of the offset and line numbers to avoid unnecessary line counting from start
		var prevLine = 1;
		var prevOffset = 0;
		while (matcher.find) {
			val task = new Task()
			val matchedTag = matcher.group(2)
			task.tag = taskTagsByName.get(if (taskTags.caseSensitive) matchedTag else matchedTag.toLowerCase)
			task.description = matcher.group(3)
			task.offset = matcher.start(2)
			task.lineNumber = Strings.countLineBreaks(source, prevOffset, task.offset) + prevLine
			prevLine = task.lineNumber
			prevOffset = task.offset
			tasks += task
		}
		tasks
	}

	protected def Map<String, TaskTag> getTaskTagsByName(TaskTags taskTags) {
		val taskTagsByName = new HashMap<String, TaskTag>
		for (tag : taskTags) {
			val name = if (taskTags.caseSensitive) tag.name else tag.name.toLowerCase
			val oldTag = taskTagsByName.get(name)
			if (oldTag !== null) {
				// prioritize higher priority tags
				if (tag.priority.ordinal < oldTag.priority.ordinal) {
					taskTagsByName.put(name, tag)
				}
			} else {
				taskTagsByName.put(name, tag)
			}
		}
		return taskTagsByName
	}

	protected def toPattern(TaskTags taskTags) {
		//this takes roughly 1�s per call, so we think its not worth caching patterns here
		var flags = Pattern.MULTILINE
		if (!taskTags.caseSensitive) {
			flags = flags.bitwiseOr(Pattern.CASE_INSENSITIVE).bitwiseOr(Pattern.UNICODE_CASE)
		}
		Pattern.compile('''^.*((�taskTags.map[Pattern.quote(name)].join("|")�)(.*)?)$''', flags)
	}
}
