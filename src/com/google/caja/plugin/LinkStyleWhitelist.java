package com.google.caja.plugin;

import java.util.Set;

import com.google.caja.util.Name;
import com.google.caja.util.Sets;

public class LinkStyleWhitelist {
  /**
   * Set of properties accessible on computed style of an anchor
   * (&lt;A&gt;) element or some element nested within an anchor. This
   * list is a conservative one based on the ability to do visibility,
   * containment, and layout calculations. It REQUIRES that user CSS
   * is prevented from specifying ANY of these properties in a history
   * sensitive manner (i.e., in a rule with a ":link" or ":visited"
   * predicate). Otherwise, it would allow an attacker to probe the
   * user's history as described at
   * https://bugzilla.mozilla.org/show_bug.cgi?id=147777 .
   */
  public static Set<Name> HISTORY_INSENSITIVE_STYLE_WHITELIST
      = Sets.immutableSet(
          Name.css("display"), Name.css("filter"), Name.css("float"),
          Name.css("height"), Name.css("left"), Name.css("opacity"),
          Name.css("overflow"), Name.css("position"), Name.css("right"),
          Name.css("top"), Name.css("visibility"), Name.css("width"),
          Name.css("padding-left"), Name.css("padding-right"),
          Name.css("padding-top"), Name.css("padding-bottom"));

  /**
   * Allowed to be specified in a history-sensitive manner in a CSS stylesheet.
   */
  public static final Set<Name> PROPERTIES_ALLOWED_IN_LINK_CLASSES;
  static {
    Set<Name> propNames = Sets.newHashSet(
        Name.css("background-color"), Name.css("color"), Name.css("cursor"));
    // Rules limited to link and visited styles cannot allow properties that
    // can be tested by Domado's getComputedStyle since it would allow history
    // mining.
    // Do not inline the below.  The removeAll relies on the input being a set
    // of names, but since removeAll takes a Collection<?> it would fail
    // silently if the whitelist were changed to a Collection<String>.
    // Assigning to a local does type-check though.
    propNames.removeAll(HISTORY_INSENSITIVE_STYLE_WHITELIST);
    PROPERTIES_ALLOWED_IN_LINK_CLASSES = Sets.immutableSet(propNames);
  }
}
