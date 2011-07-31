#!/usr/bin/ruby
# -*- coding: utf-8 -*-
require 'optparse'
require 'open3'

if $0 == __FILE__
  unless ARGV.length == 2
    puts "Usage: #{File.basename $0} csp result"
    exit
  end
  cspFile, result = ARGV

  io = IO.popen('sugar -vv /dev/stdin', 'r+')
  if cspFile.match /\.xml$/
    csp = IO.popen("../tools/xml2csp #{cspFile}")
  else
    csp = open(cspFile)
  end
  csp.each { |line|
    io.puts line
  }
  begin
    open(result).each { |line|
      if line.match /^s UNSATISFIABLE$/
        warn "result is UNSAT: cannot validate."
        raise "end"
      elsif line.match /^s UNKNOWN$/
        warn "result is UNKNOWN: cannot validate."
        raise "end"
      elsif line.match /^a\s+[^\s]+\s+[^\s]+$/
        var, val = line.split[1..2]
        if val.match /^true$/
          io.puts var
        elsif val.match /^false$/
          io.puts "(! #{var})"
        else
          io.puts "(= #{var} #{val})"
        end
      end
    }
    io.close_write

    io.each{ |line|
      if line.match /^s SATISFIABLE$/
        raise "end"
      elsif line.match /^s UNKNOWN$/
        warn "Error: invalid input"
        raise "end"
      end
    }
    warn "Error: wrong answer."
  rescue
    Process.kill :INT, io.pid
  end
end
