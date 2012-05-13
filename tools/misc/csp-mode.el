;;; csp-mode.el --- Extension for editing CSP files for Azucar solver

;; Copyright (C) 2012  by Tomoya Tanjo

;; Author: Tomoya Tanjo <tanjo@nii.ac.jp>
;; Keywords: major-mode, csp

(require 'generic)

(define-generic-mode csp-mode
    '(";")
    '("int" "bool" "domain"
      "predicate" "relation" "objective"
      "not" "!" "and" "&&" "or" "||"
      "imp" "=>" "xor" "iff" "<=>"
      "eq" "=" "ne" "!="
      "le" "<=" "lt" "<" "ge" ">=" "gt" ">"
      "alldifferent" "weightedsum"
      "cumulative" "element" "disjunctive"
      "lex_less" "lex_lesseq" "nvalue"
      "count" "global_cardinality"
      "global_cardinality_with_costs"
      "false" "true")
    `((,(regexp-opt '("minimize" "maximize" "supports" "conflicts"
                      "nil")) .
        font-lock-constant-face)
      (,(concat "\\b" (regexp-opt '("neg" "abs" "add" "sub"
                                    "mul" "div" "mod"
                                    "pow" "min" "max" "if"))
                "\\b") .
        font-lock-function-name-face)
      (,(regexp-opt '("-" "+" "-" "*" "/" "%")) .
        font-lock-function-name-face)
      ("\\b[0-9]+\\b" . font-lock-constant-face))
    '("\\.csp\\'")
    '(define-csp-keymap)
    "Major mode for csp")

(defvar csp-local-map nil "Keymap for csp-mode")

(defun define-csp-keymap ()
  (setq csp-local-map (make-sparse-keymap "CSP"))
  (set-keymap-parent csp-local-map lisp-mode-shared-map)
  (use-local-map csp-local-map))

(provide 'csp-mode)
