/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.grammaranalysis.impl;

import static com.google.common.collect.Iterables.*;
import static org.eclipse.xtext.GrammarUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CompoundElement;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.UnorderedGroup;
import org.eclipse.xtext.grammaranalysis.IGrammarNFAProvider.NFABuilder;
import org.eclipse.xtext.grammaranalysis.INFAState;
import org.eclipse.xtext.grammaranalysis.INFATransition;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class AbstractNFAState<S extends INFAState<S, T>, T extends INFATransition<S, T>> extends AdapterImpl implements
		INFAState<S, T> {

	protected List<T> allIncoming;

	protected final NFABuilder<S, T> builder;

	protected final AbstractElement element;

	protected boolean endState = false;

	protected List<T> outgoing;

	protected List<T> outgoingRuleCalls;

	public AbstractNFAState(AbstractElement element, NFABuilder<S, T> builder) {
		super();
		this.element = element;
		this.builder = builder;
	}

	@SuppressWarnings("unchecked")
	protected void addOutgoing(AbstractElement ele, Set<AbstractElement> visited, boolean isRuleCall,
			AbstractElement loopCenter) {
		if (!visited.add(ele))
			return;
		if (filter(ele))
			collectOutgoing(ele, visited, isRuleCall, loopCenter);
		else {
			if (isRuleCall)
				outgoingRuleCalls.add(builder.getTransition((S) this, builder.getState(ele), true, loopCenter));
			else
				outgoing.add(builder.getTransition((S) this, builder.getState(ele), false, loopCenter));
		}
	}

	protected void collectAllOutgoingTransitions() {
		if (filter(element))
			outgoing = outgoingRuleCalls = Collections.emptyList();
		else {
			outgoing = new ArrayList<T>();
			outgoingRuleCalls = new ArrayList<T>();
			collectOutgoing(element, Sets.<AbstractElement> newHashSet(), false, null);
			removeDuplicates(outgoing);
			removeDuplicates(outgoingRuleCalls);
		}
	}

	protected void collectOutgoing(AbstractElement element, Set<AbstractElement> visited, boolean isRuleCall,
			AbstractElement loopCenter) {
		if (element instanceof Group || element instanceof UnorderedGroup) {
			List<AbstractElement> elements = ((CompoundElement) element).getElements();
			switch (builder.getDirection()) {
				case FORWARD:
					int j = 0;
					while (j < elements.size()) {
						addOutgoing(elements.get(j), visited, isRuleCall, loopCenter);
						if (!GrammarUtil.isOptionalCardinality(elements.get(j)))
							break;
						j++;
					}
					// add transitions to the next sibling of this element if all children are optional
					if (j >= elements.size())
						collectOutgoingByContainer(element, visited, isRuleCall, loopCenter);
					break;
				case BACKWARD:
					int i = elements.size() - 1;
					while (i >= 0) {
						addOutgoing(elements.get(i), visited, isRuleCall, loopCenter);
						if (!GrammarUtil.isOptionalCardinality(elements.get(i)))
							break;
						i--;
					}
					// add transitions to the next sibling of this element if all children are optional
					if (i < 0)
						collectOutgoingByContainer(element, visited, isRuleCall, loopCenter);
					break;
			}
		} else if (element instanceof Alternatives)
			for (AbstractElement e : ((Alternatives) element).getElements())
				addOutgoing(e, visited, isRuleCall, loopCenter);
		else if (element instanceof Assignment)
			addOutgoing(((Assignment) element).getTerminal(), visited, isRuleCall, loopCenter);
		else if (element instanceof CrossReference)
			addOutgoing(((CrossReference) element).getTerminal(), visited, isRuleCall, loopCenter);
		else if (element instanceof RuleCall
				&& ((RuleCall) element).getRule().getType().getClassifier() instanceof EClass) {
			addOutgoing(((RuleCall) element).getRule().getAlternatives(), visited, true, loopCenter);
			collectOutgoingByContainer(element, visited, isRuleCall, loopCenter);
		} else {
			if (GrammarUtil.isMultipleCardinality(element) && !filter(element))
				addOutgoing(element, visited, isRuleCall, element);
			collectOutgoingByContainer(element, visited, isRuleCall, loopCenter);
		}
	}

	protected void collectOutgoingByContainer(AbstractElement element, Set<AbstractElement> visited,
			boolean isRuleCall, AbstractElement loopCenter) {
		EObject container = element.eContainer();
		if (container instanceof Group || container instanceof UnorderedGroup) {
			CompoundElement compoundContainer = (CompoundElement) container;
			List<AbstractElement> siblings = compoundContainer.getElements();
			int i = siblings.indexOf(element);
			switch (builder.getDirection()) {
				case FORWARD:
					if ((i + 1) >= siblings.size()) {
						if (GrammarUtil.isMultipleCardinality(compoundContainer))
							addOutgoing(compoundContainer, visited, isRuleCall, compoundContainer);
						collectOutgoingByContainer(compoundContainer, visited, isRuleCall, loopCenter);
					} else {
						AbstractElement next = siblings.get(i + 1);
						addOutgoing(next, visited, isRuleCall, loopCenter);
						if (GrammarUtil.isOptionalCardinality(next))
							collectOutgoingByContainer(next, visited, isRuleCall, loopCenter);
					}
					break;
				case BACKWARD:
					if (i <= 0) {
						if (GrammarUtil.isMultipleCardinality(compoundContainer))
							addOutgoing(compoundContainer, visited, isRuleCall, compoundContainer);
						collectOutgoingByContainer(compoundContainer, visited, isRuleCall, loopCenter);
					} else {
						AbstractElement next = siblings.get(i - 1);
						addOutgoing(next, visited, isRuleCall, loopCenter);
						if (GrammarUtil.isOptionalCardinality(next))
							collectOutgoingByContainer(next, visited, isRuleCall, loopCenter);
					}
					break;
			}
		} else if (container instanceof AbstractRule)
			endState = true;
		else if (container instanceof AbstractElement) {
			AbstractElement elementContainer = (AbstractElement) container;
			if (GrammarUtil.isMultipleCardinality(elementContainer))
				addOutgoing(elementContainer, visited, isRuleCall, elementContainer);
			collectOutgoingByContainer(elementContainer, visited, isRuleCall, loopCenter);
		} else
			throw new IllegalStateException("Unknown container: " + container);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void collectReferencesToThis(S match, Set<Object> visited, List<T> following) {
		if (!visited.add(this))
			return;
		for (T transition : concat(getOutgoing(), getOutgoingAfterReturn()))
			if (!transition.isRuleCall()) {
				((AbstractNFAState) transition.getTarget()).collectReferencesToThis(match, visited, following);
				if (transition.getTarget() == match)
					following.add(transition);
			}
	}

	protected boolean filter(AbstractElement ele) {
		AbstractRule rule = GrammarUtil.containingRule(ele);
		if (rule == null || !(rule.getType().getClassifier() instanceof EClass))
			return true;
		return builder.filter(ele);
	}

	public List<T> getAllIncoming() {
		if (allIncoming != null)
			return allIncoming;
		allIncoming = Lists.newArrayList();
		for (EObject root : element.eResource().getContents())
			if (root instanceof Grammar)
				for (AbstractRule rule : ((Grammar) root).getRules())
					if (rule.getType().getClassifier() instanceof EClass)
						for (AbstractElement ele : EcoreUtil2.eAllOfType(rule, AbstractElement.class))
							if (!builder.filter(ele))
								for (T t : builder.getState(ele).getAllOutgoing())
									if (t.getTarget() == this)
										allIncoming.add(t);
		return allIncoming;
	}

	public List<T> getAllOutgoing() {
		if (outgoing == null || outgoingRuleCalls == null)
			collectAllOutgoingTransitions();
		List<T> result = Lists.newArrayListWithExpectedSize(outgoing.size() + outgoingRuleCalls.size());
		result.addAll(outgoing);
		result.addAll(outgoingRuleCalls);
		return result;
	}

	public NFABuilder<S, T> getBuilder() {
		return builder;
	}

	public AbstractElement getGrammarElement() {
		return element;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<T> getIncommingWithoutRuleCalls() {
		List<T> result = Lists.newArrayList();
		AbstractElement rootEle = containingRule(element).getAlternatives();
		((AbstractNFAState) builder.getState(rootEle)).collectReferencesToThis(this, Sets.newHashSet(), result);
		return result;
	}

	public List<T> getOutgoing() {
		if (outgoing == null || outgoingRuleCalls == null)
			collectAllOutgoingTransitions();
		return outgoingRuleCalls.isEmpty() ? outgoing : outgoingRuleCalls;
	}

	public List<T> getOutgoingAfterReturn() {
		if (outgoing == null || outgoingRuleCalls == null)
			collectAllOutgoingTransitions();
		return outgoingRuleCalls.isEmpty() ? Collections.<T> emptyList() : outgoing;
	}

	public boolean isEndState() {
		if (outgoing == null || outgoingRuleCalls == null)
			collectAllOutgoingTransitions();
		return endState;
	}

	public boolean isStartState() {
		return element.eContainer() instanceof AbstractRule;
	}

	protected void removeDuplicates(List<T> list) {
		for (int i = list.size() - 2; i >= 0; i--) {
			for (int j = i + 1; j < list.size(); j++) {
				if (list.get(i).equals(list.get(j))) {
					list.remove(i);
					break;
				}
			}
		}
	}

}