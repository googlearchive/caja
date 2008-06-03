# makefile -- 
# Author          : Jasvir Nagra <jas@cs.auckland.ac.nz>
# Created On      : Wed May  1 02:49:53 1996
# Last Modified   : <08/06/02 15:28:13 jasvir>
# Description     : Document Caja
# Keywords        : caja specification
# Purpose         : Research

# Main document
MAIN=caja-spec.tex  

# Sections
SRCS= 

# Figures
INKSCAPE_FIGS=1.svg  2.svg  3.svg  4.svg  5.svg  6.svg  dialog.svg  seesaw-pola.svg

# Deduced variables
MAIN_PDF=$(MAIN:.tex=.pdf)
PST_FIGS=$(FIG_FIG$:.fig=.pstex_t)
PS_FIGS=$(FIG_FIG$:.fig=.pstex)
FIGS= $(PST_FIGS) $(PS_FIGS) $(FIG_FIGS) $(INKSCAPE_FIGS)

all:          $(MAIN_PDF)

view:	$(MAIN_PDF)
	xpdf $< &

caja-spec.pdf: $(MAIN) $(SRC)
	pdflatex $(MAIN)

clean:
	@rm -f $(MAIN:.tex=.aux) \
                          $(MAIN:.tex=.bbl) \
                          $(MAIN:.tex=.blg) \
                          $(MAIN:.tex=.brf) \
                          $(MAIN:.tex=.dvi) \
                          $(MAIN:.tex=.idx) \
                          $(MAIN:.tex=.lof) \
                          $(MAIN:.tex=.log) \
                          $(MAIN:.tex=.lot) \
                          $(MAIN:.tex=.out) \
                          $(MAIN:.tex=.pdf) \
                          $(MAIN:.tex=.toc) \
                          $(FIGS)

# Common generators
$(INKSCAPE_FIGS:.svg=.pdf): $(INKSCAPE_FIGS)
	@echo Warning: Inkscape figures out of date. Re-export from inkscape.

%.fig:            %.wmf
	wmf2fig -o $@ $<

%.pstex:          %.fig $(EPS_FIG)
	fig2dev -L pstex $< $@

%.pstex_t:        %.fig %.pstex
	fig2dev -L pstex_t -p $(<:.fig=.pstex) $< $@

# Emacs bunkum
# Local Variables:
# mode: makefile
# time-stamp-start: "Last Modified[ \t]*:[ 	]+\\\\?[\"<]+"
# time-stamp-end:   "\\\\?[\">]"
# End:
