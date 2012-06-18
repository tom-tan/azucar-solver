#!/usr/bin/ruby
# -*- coding: utf-8 -*-
require 'optparse'

class Operation
  attr_reader :stime, :ptime, :number
  def initialize(s, pt, num)
    @stime = s
    @ptime = pt
    @number = num
  end

  def to_s
    "Operation(stime: #{@stime}, ptime: #{@ptime}, number: #{@number})"
  end
end

def calc_lower_bound(ops, jobn)
  [*ops.map{ |os|
    os.map{ |o| o.ptime }.reduce(:+)
  },
  *jobn.values.map{ |os|
    os.map{ |o| o.ptime }.reduce(:+)
  }].max
end

def calc_upper_bound(jss)
  `java -cp .:cream105.jar JSSP < #{jss}`.chomp
end


if $0 == __FILE__
  out = STDOUT
  makespan = nil
  isCOP = true

  opt = ARGV.options{ |o|
    o.banner = "Usage: #{File.basename $0} [options] jss"
    o.on('-o OUTPUT', 'Output file name (Default: stdout)', String) { |out_|
      out = out_
    }
    o.on('-m Makespan', 'Set Makespan to makespan', Integer) { |m|
      makespan = m
      isCOP = false
    }
  }
  opt.parse!
  unless ARGV.length == 1
    puts opt.help
    exit
  end
  input = ARGV.pop

  ops = open(input) { |io|
    m, n = io.gets.split.map{ |s| s.to_i }
    ops = io.readlines.each_with_index.map{ |line, i|
      line.split.map{ |s| s.to_i }.each_slice(2).each_with_index.map{ |num_ptime, j|
        Operation.new("s_#{i}_#{j}", num_ptime[1], num_ptime[0])
      }
    }
  }

  jobn = Hash.new([].freeze)
  ops.each{ |ops_per_jobs|
    ops_per_jobs.each{ |o|
      jobn[o.number] += [o]
    }
  }
  lb = calc_lower_bound(ops, jobn)
  ub = makespan.nil? ? calc_upper_bound(input) : makespan

  out = (out == STDOUT) ? STDOUT : open(out, 'w')
  out.puts "(int makespan #{lb} #{ub})"
  out.puts "(objective minimize makespan)" if isCOP
  ops.each{ |ops_per_jobs|
    ops_per_jobs.each{ |o|
      out.puts "(int #{o.stime} 0 #{ub})"
    }
    (ops_per_jobs+[Operation.new('makespan', nil, nil)]).each_cons(2) { |o1, o2|
      out.puts "(<= (+ #{o1.stime} #{o1.ptime}) #{o2.stime})"
    }
  }
  jobn.each_value{ |os|
    os.combination(2)  { |o1, o2|
      out.puts "(or (<= (+ #{o1.stime} #{o1.ptime}) #{o2.stime}) (<= (+ #{o2.stime} #{o2.ptime}) #{o1.stime}))"
    }
  }
end
