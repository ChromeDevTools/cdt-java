// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var adviserImpl = (function() {
	function Data() {
		this.chromiumVersions = [
				new VersionRange(new Version([ 1 ]), new Version(
						[ 9, Infinity ])),
				new VersionRange(new Version([ 10 ]), new Version(
						[ 10, Infinity ])),
				new VersionRange(new Version([ 11 ]),
						new Version([ Infinity ]), "future versions"), ]
				.sort(VersionRange.compareStrict);

		this.toolsRealVersions = [ new Version([ 0, 1, 0 ]),
				new Version([ 0, 1, 2 ]), new Version([ 0, 1, 4 ]),
				new Version([ 0, 1, 6 ]), new Version([ 0, 2, 0 ]),
				new Version([ 0, 2, 2 ]) ];

		var toolsVersions = [
				new VersionRange(new Version([ 0, 2, 3 ]),
						new Version([ Infinity ]), "future versions"), ];
		this.toolsRealVersions.forEach(function(version) {
			toolsVersions.push(createPointRange(version));
		});
		toolsVersions.sort(VersionRange.compareStrict);
		this.toolsVersions = toolsVersions;

		this.constraints = [
				new Constraint(new VersionRange(new Version([ 5, 0, 342, 0 ]),
						new Version([ 5, 0, 366, 2 ])), new VersionRange(
						new Version([ 0 ]), new Version([ Infinity ])),
						"Debug protocol is broken in Chrome", false),
				new Constraint(new VersionRange(new Version([ 7, 0, 511, 4 ]),
						new Version([ Infinity ])), new VersionRange(
						new Version([ 0 ]), new Version([ 0, 2, 0 ])),
						"Protocol is incompatible for non-ASCII characters",
						false), ];
	}

	function Version(components) {
		this.components_ = components;
	}
	Version.prototype.getComponent = function(pos) {
		if (pos < this.components_.length) {
			return this.components_[pos];
		} else {
			return -Infinity;
		}
	};
	Version.prototype.getLength = function() {
		return this.components_.length;
	};
	Version.prototype.toString = function() {
		return "[" + this.components_.toString() + "]";
	};
	Version.prototype.getHtmlText = function(versionFormat) {
		var res = "";
		var len = Math.max(versionFormat.length, this.components_.length);
		var defaultValue = void 0;
		for ( var i = 0; i < len; i++) {
			if (res.length > 0) {
				res += ".";
			}
			var comp = this.components_[i];
			if (comp === void 0) {
				if (defaultValue === void 0) {
					defaultValue = 0;
				}
				comp = defaultValue;
			}
			if (comp == Infinity) {
				res += "&infin;";
				defaultValue = Infinity;
			} else {
				res += comp;
			}
		}
		return res;
	};
	Version.prototype.adjoinsWith = function(other) {
		if (Version.compare(this, other) == 0) {
			return true;
		}
		var limit = Math.max(this.getLength(), other.getLength());
		var pos = 0;
		while (pos < limit) {
			if (this.components_[pos] == other.components_[pos]) {
				pos++;
				continue;
			}
			if (this.components_[pos] + 1 == other.components_[pos]
					&& pos + 1 < limit && this.components_[pos + 1] == Infinity) {
				if (pos + 1 >= other.components_.length
						|| other.components_[pos + 1] == 0) {
					return true;
				}
			}
			return false;
		}
	};

	Version.compare = function(version1, version2) {
		var pos = 0;
		var limit = Math.max(version1.getLength(), version2.getLength());
		while (pos < limit) {
			var res = version1.getComponent(pos) - version2.getComponent(pos);
			if (res < 0) {
				return -1;
			} else if (res > 0) {
				return +1;
			}
			pos++;
		}
		return 0;
	};
	Version.parseString = function(str) {
		var result = [];
		str.split(".").forEach(function(item) {
			result.push(new Number(item).valueOf());
		});
		return new Version(result);
	};

	function VersionRange(start, end, opt_codeName) {
		this.start = start;
		this.end = end;
		this.codeName_ = opt_codeName;
		if (Version.compare(start, end) > 0) {
			throw "Start > End";
		}
	}
	VersionRange.prototype.toString = function() {
		return this.start + " - " + this.end;
	};
	VersionRange.prototype.getHtmlText = function(versionFormat) {
		var res = "";
		if (Version.compare(this.start, this.end) == 0) {
			res += this.start.getHtmlText(versionFormat);
		} else {
			res += this.start.getHtmlText(versionFormat) + " &mdash; "
					+ this.end.getHtmlText(versionFormat);
		}
		if (this.codeName_) {
			res += " (" + this.codeName_ + ")";
		}
		return res;
	};

	VersionRange.prototype.contains = function(version) {
		if (Version.compare(this.end, this.start) == 0) {
			return Version.compare(this.end, version) == 0;
		}
		return Version.compare(version, this.start) >= 0
				&& Version.compare(version, this.end) < 0;
	};
	VersionRange.prototype.isPoint = function() {
		return Version.compare(this.start, this.end) == 0;
	};

	VersionRange.prototype.overlap = function(range) {
		var rangeBeforeThis = void 0;
		var point1;
		var rangeOverlap = void 0;
		var point2;
		var rangeAfterThis = void 0;

		if (this.isPoint()) {
			if (range.isPoint()) {
				var compareResult = Version.compare(this.start, range.start);
				if (compareResult == 0) {
					rangeOverlap = range;
				} else if (compareResult < 0) {
					rangeAfterThis = range;
				} else {
					rangeBeforeThis = range;
				}
			} else {
				if (Version.compare(this.start, range.end) >= 0) {
					rangeBeforeThis = range;
				} else if (Version.compare(this.start, range.start) >= 0) {
					rangeBeforeThis = new VersionRange(range.start, this.start);
					rangeOverlap = this;
					rangeAfterThis = new VersionRange(this.start, range.end);
				} else {
					rangeAfterThis = range;
				}
			}
		} else {
			if (range.isPoint()) {
				if (Version.compare(this.start, range.start) > 0) {
					rangeBeforeThis = range;
				} else if (Version.compare(this.end, range.start) > 0) {
					rangeOverlap = range;
				} else {
					rangeAfterThis = range;
				}
			} else {
				var sameOverlapInstance = true;
				point1 = range.start;
				point2 = range.end;

				if (Version.compare(range.start, this.start) < 0) {
					point1 = this.start;
					sameOverlapInstance = false;
					if (Version.compare(range.end, this.start) <= 0) {
						rangeBeforeThis = range;
					} else {
						rangeBeforeThis = new VersionRange(range.start,
								this.start);
					}
				}

				if (Version.compare(range.end, this.end) > 0) {
					point2 = this.end;
					sameOverlapInstance = false;
					if (Version.compare(range.start, this.end) >= 0) {
						rangeAfterThis = range;
					} else {
						rangeAfterThis = new VersionRange(this.end, range.end);
					}
				}

				if (Version.compare(point1, point2) <= 0) {
					if (sameOverlapInstance) {
						rangeOverlap = range;
					} else {
						rangeOverlap = new VersionRange(point1, point2);
					}
				}
			}
		}

		return {
			before : rangeBeforeThis,
			overlap : rangeOverlap,
			after : rangeAfterThis
		};
	};
	VersionRange.prototype.mergeWith = function(next) {
		if (this.end.adjoinsWith(next.start) && this.codeName == next.codeName &&
				this.codeName_ == next.codeName_) {
			return new VersionRange(this.start, next.end);
		}
	};

	VersionRange.compareStrict = function(range1, range2) {
		if (Version.compare(range1.start, range2.end) > 0) {
			return +1;
		} else if (Version.compare(range2.start, range1.end) > 0) {
			return -1;
		} else {
			throw "Overlapping ranges: " + range1 + " - " + range2;
		}
	};
	VersionRange.compareStrict = function(range1, range2) {
		return Version.compare(range1.start, range2.start) == 0 &&
            Version.compare(range1.end, range2.end);
	};
	function createPointRange(version, opt_codeName) {
		return new VersionRange(version, version, opt_codeName);
	}

	function Constraint(chromeRange, toolRange, explanation, isOk) {
		this.chromeRange = chromeRange;
		this.toolRange = toolRange;
		this.explanation = explanation;
		this.isOk = isOk;
	}
	Constraint.prototype.getRawDescriptionHtml = function(chromeVersionFormat,
			toolsVersionFormat) {
		return "chromeVersions=" + this.chromeRange + " toolsVersions="
				+ this.toolRange + " explanation='" + this.explanation + "'";
	};

	function UseReport(range, comment) {
		this.range = range;
		this.comment = comment;
	}
	UseReport.prototype.getHtmlTableRowText = function(handlers) {
		var res = "<tr><td>" + this.range.getHtmlText(handlers.versionFormat)
				+ "</td>";
		var statusText;
		var commentText;
		if (this.comment) {
			statusText = "Doesn't work";
			commentText = this.comment;
		} else {
			statusText = "<b>OK</b>";
			commentText = "";
		}
		res += "<td>" + statusText + "</td><td>" + commentText + "</td></tr>";
		return res;
	};
	UseReport.prototype.mergeWith = function(next) {
		var mergedRange = this.range.mergeWith(next.range);
		if (mergedRange && this.comment == next.comment) {
			return new UseReport(mergedRange, this.comment);
		}
	};
	UseReport.prototype.toString = function() {
		return "UseReport<" + this.range + " : " + this.comment + ">";
	};
	UseReport.getTableHeaderHtml = function(handlers) {
		return "<tr><th>Version of " + handlers.opposite().fromName
				+ " </th><th>Status</th><th>Comments</th></tr>\n";
	};

	var data = new Data();

	var fromChromeToToolsHandlers = {
		getFromConstraintRange : function(constraint) {
			return constraint.chromeRange;
		},
		getToConstraintRange : function(constraint) {
			return constraint.toolRange;
		},
		getFromRangeList : function() {
			return data.chromiumVersions;
		},
		getToRangeList : function() {
			return data.toolsVersions;
		},
		versionFormat : {
			length : 3
		},
		fromName : "Chromium/Google Chrome",
		opposite : function() {
			return fromToolsToChromeHandlers;
		}
	};
	var fromToolsToChromeHandlers = {
		getFromConstraintRange : function(constraint) {
			return constraint.toolRange;
		},
		getToConstraintRange : function(constraint) {
			return constraint.chromeRange;
		},
		getFromRangeList : function() {
			return data.toolsVersions;
		},
		getToRangeList : function() {
			return data.chromiumVersions;
		},
		versionFormat : {
			length : 4
		},
		fromName : "Chrome Developer Tools for Java",
		opposite : function() {
			return fromChromeToToolsHandlers;
		}
	};

	function calculateUseReports(fromVersion, toRangeList, handlers) {
		var result = [];
		toRangeList.forEach(function(range) {
			result.push(new UseReport(range));
		});

		data.constraints.forEach(function(constr) {
			if (handlers.getFromConstraintRange(constr).contains(fromVersion)) {
				var newResult = [];
				result.forEach(function(useReport) {
					var overlapRes = handlers.getToConstraintRange(constr)
							.overlap(useReport.range);
					if (overlapRes.overlap) {
						if (overlapRes.before) {
							newResult.push(new UseReport(overlapRes.before));
						}
						newResult.push(new UseReport(overlapRes.overlap,
								constr.explanation));
						if (overlapRes.after) {
							newResult.push(new UseReport(overlapRes.after));
						}
					} else {
						newResult.push(useReport);
					}
				});
				result = newResult;
			}
		});
		return result;
	}

	function calculateResultHtml(versionString, handlers) {
		var found = false;

		var fromVersion = Version.parseString(versionString);

		handlers.getFromRangeList().forEach(function(range) {
			if (range.contains(fromVersion)) {
				found = true;
			}
		});

		if (!found) {
			throw "Unknown version number: "
					+ fromVersion
							.getHtmlText(handlers.opposite().versionFormat);
		}

		var useList = calculateUseReports(fromVersion, handlers
				.getToRangeList(), handlers);

		if (useList.length > 0) {
			var prev = 0;
			for ( var i = 1; i < useList.length; i++) {
				var newUseReport = useList[prev].mergeWith(useList[i]);
				if (newUseReport) {
					useList[prev] = newUseReport;
				} else {
					prev++;
					useList[prev] = useList[i];
				}
			}
			useList.length = prev + 1;
		}

		useList.reverse();

		var text = "";

		text += "Version of " + handlers.fromName + ": <i>"
				+ fromVersion.getHtmlText(handlers.opposite().versionFormat)
				+ "</i>\n";
		text += "<p>";

		text += "<table style='border-spacing: 2em 0'>"
				+ UseReport.getTableHeaderHtml(handlers);
		useList.forEach(function(use) {
			text += use.getHtmlTableRowText(handlers) + "\n";
		});
		text += "</table>\n";

		text += "<div style='padding: 4em 0 0 0;'></div>\n";
		return text;
	}

	function calculateAndPrint(versionString, output, handlers) {
		var resHtml;
		try {
			resHtml = calculateResultHtml(versionString, handlers);
		} catch (e) {
			resHtml = "Error: " + e;
		}
		output.innerHTML = resHtml;
	}

	function getAllLimitationsHtml() {
		var res = "";
		data.constraints.forEach(function(constr) {
			res += constr.getRawDescriptionHtml(
					fromChromeToToolsHandlers.versionFormat,
					fromToolsToChromeHandlers.versionFormat);
			res += "<br>\n";
		});
		return res;
	}
	function getKnownToolsVersions() {
		var result = [];
		data.toolsRealVersions.forEach(function(version) {
			result.push(version
					.getHtmlText(fromChromeToToolsHandlers.versionFormat));
		});
		return result;
	}

	function runTests() {

	}

	return {
		CalculateAndPrintToolsVersions : function(versionString, output) {
			return calculateAndPrint(versionString, output,
					fromChromeToToolsHandlers);
		},
		CalculateAndPrintChromeVersions : function(versionString, output) {
			return calculateAndPrint(versionString, output,
					fromToolsToChromeHandlers);
		},
		GetKnownToolsVersions : getKnownToolsVersions,
		DumpAllLimitations : getAllLimitationsHtml,
		RunTest : runTests()
	};
})();
