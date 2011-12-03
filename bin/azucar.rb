#!/usr/bin/ruby
# -*- coding: utf-8 -*-
require 'optparse'
require 'tempfile'

Version = 'v0.0.0'

class Logger
  Verbose = 1
  VeryVerbose = 2

  def initialize(verbose)
    @time0 = Time.now
    @verbose = verbose
  end

  def log(*args)
    str = args.join ' '
    if str.match(/ERROR/i) or @verbose >= Verbose
      Kernel.puts "c #{Time.now-@time0}\t#{str}"
    end
  end

  def vlog(*args)
    if @verbose >= VeryVerbose
      self.log args
    end
  end

  def plog(line)
    case line
    when /^c (.*)$/
      vlog $1
    when /^s (.*)$/
      puts line
    when /^o (.*)$/
      puts line
    when /^\w\b/
      puts line
    else
      log line
    end
  end

  def warn(*args)
    log "c ERROR #{args.join ' '}"
  end
end

class Java
  def initialize(java, jar, opt, debugLv, verboseLv, prof, logger)
    @cmd = "#{java} -cp '#{jar}'"
    @cmd += ' -v' if verboseLv >= Verbose
    @cmd += ' -v' if verboseLv >= VeryVerbose
    @cmd += " -option #{opt}" unless opt.nil?
    @cmd += " -debug #{debugLv}" if debugLv >= 1
    unless prof.nil?
      @cmd += " -agentlib:hprof=cpu=samples,depth=8,file=#{prof}"
    end
    @logger = logger
  end

  def run(jopt, cls, args)
    cmd = "#{@cmd} #{jopt} #{cls} #{args}"
    @logger.vlog "CMD #{cmd}"
    begin
      io = IO.popen("#{cmd} 2>&1")
      io.each{ |line|
        @logger.plog line
      }
    rescue Interrupt
      # sigterm
      unless io.nil? or io.closed?
        Signal.trap :INT, 'IGNORE'
        Signal.trap :TERM, 'IGNORE'
        Process.kill :INT, -Process.pid
        Signal.trap :INT, 'DEFAULT'
        Signal.trap :TERM, 'DEFAULT'
        io.each{ |line|
          @logger.plog line
        }
      end
    ensure
      io.close
    end
  end

  def convert(jopt1, xml, csp)
    @logger.log "CONVERTING #{xml} TO #{csp}"
    unless File.exist? xml
      @logger.warn "no XML file #{xml}"
      return
    end
    run jopt1, "jp.ac.kobe_u.cs.sugar.XML2CSP", xml, csp
  end
end


if $0 == __FILE__
  java_cmd = 'java'
  jar = "/home/tomoya/azucar/bin/azucar-#{Version}.jar"
  solver0 = 'minisat'
  solver0_inc = 'minisat-inc'
  tmp = "/tmp/azucar#{Process.pid}"
  debugLv = 0
  verboseLv = 0
  competition = false
  incremental = false
  option = nil
  max_csp = false
  csp_file = ''
  sat_file = ''
  map_file = ''
  out_file = ''
  java_opt1 = ''
  java_opt2 = ''
  prof = nil
  keep = false
  memlimit = nil
  encoding = 'coe'
  m = 2
  b = nil
  enable_assert = false
  propagate = true

  solver = nil

  opt = ARGV.options{ |o|
    o.banner = "#{$0} [options] csp_file"
    o.on('-debug level', 'debug option', Integer) { |lv|
      debug = lv
    }
    o.on('-v', '-verbose', 'verbose output') {
      verboseLv = [verboseLv, 1].max
    }
    o.on('-vv', '-veryverbose', 'verbose output') {
      verboseLv = [verboseLv, 2].max
    }
    o.on('-c', '-competition', 'CSP Solver Competition mode') {
      competition = true
    }
    o.on('-i', '-incremental', 'use incremental search (you need a patched SAT solver)') {
      incremental = true
    }
    o.on('-o', '-option o1,...', 'set option (e.g. -pigeon)',
         String) { |opts|
      option = opts
    }
    o.on('-max', 'MAX-CSP mode') {
      max_csp = true
    }
    o.on('-tmp prefix', 'path and prefix of temporary files',
         String) { |pfx|
      tmp = pfx
    }
    o.on('-csp file', 'output CSP file name for XCSP input',
         String) { |f|
      csp_file = f
    }
    o.on('-sat file', 'SAT problem file name', String) { |f|
      sat_file = f
    }
    o.on('-map file', 'mapping file name', String) { |f|
      map_file = f
    }
    o.on('-out file', 'SAT solver output file name', String) { |f|
      out_file = f
    }
    o.on('-java command', "java command (default: #{java_cmd})",
         String) { |cmd|
      java_cmd = cmd
    }
    o.on('-jopt1 option', 'java option for encoding', String) { |opt|
      java_opt1 = opt
    }
    o.on('-jopt2 option', 'java option for decoding', String) { |opt|
      java_opt2 = opt
    }
    o.on('-jar file', "jar file name to be used (default: #{jar})",
         String) { |j|
      jar = j
    }
    o.on('-prof file', 'java CPU profiling', String) { |f|
      prof = f
    }
    o.on('-solver command', "SAT solver command (default: #{solver0})",
         String) { |cmd|
      solver = cmd
    }
    o.on('-keep', 'do not erase temporary files') {
      keep = true
    }
    o.on('-memlimit MiB', 'memory limit in MiB (for CSP solver competition)',
         Integer) { |mb|
      memlimit = mb
    }
    o.on('-encoding enc', 'encoding method (oe and coe are supported)', String) { |enc|
      encoding = enc
    }
    o.on('-m m', 'the number of digits (only for coe)', Integer) { |m_|
      m = m_
    }
    o.on('-b B', 'uses a numeral system of base B (only for coe)',
         Integer) { |b_|
      b = b_
    }
    o.on('-ea', 'enable assertion') {
      enable_assert = true
    }
    o.on('-nopropagate', 'do not propagate') {
      propagate = true
    }
  }
  opt.parse!
  unless ARGV.length == 1
    puts opt.help
    exit
  end

  if solver.nil?
    solver = incremental ? solver0_inc : solver0
  end
  if java_opt1.nil? and memlimit > 200
    java_opt1 = "-Xmx#{memlimit-200}M"
  end
  if java_opt2.nil?
    if incremental
      java_opt2 = "-Xmx50M"
      solverlimit = memlimit-250 if memlimit > 250
    elsif memlimit > 200
      java_opt2 = "-Xmx#{memlimit-200}M"
    end
  end

  if enable_assert
    java_opt1 += ' -ea'
    java_opt2 += ' -ea'
  end

  input = ARGV.pop
  unless File.exist? input
    raise "#{$0}: no input file #{input}"
  end

  if input.match /\.xml(\.gz)?$/
    xml_file = input
    csp_file = "#{tmp}.csp"
  else
    xml_file = nil
    csp_file = input
  end

  logger = Logger.new(verboseLv)
  java = Java.new(java_cmd, jar, option, debug, verboseLv, prof, logger)

  File.unlink csp_file unless xml_file.nil?
  File.unlink map_file, sat_file, out_file

  logger.log "Azucar #{Version} + #{solver}"
  logger.log "BEGIN #{Time.now}"

  logger.vlog "PID #{Process.pid}"
  logger.vlog "HOST #{`hostname`.chomp}"

  unless xml_file.nil?
    java.convert(java_opt1, xml_file, csp_file)
    # fail
  end

  unless File.exist? csp_file
    raise "#{csp_file} not found"
  end

  java.encode(java_opt1, csp_file, sat_file, map_file)
  solve(sat_file, out_file, map_file) if ! result
end
