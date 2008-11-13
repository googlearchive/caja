module Caja
  def self.class_path
    files = Dir["#{self.src_path}/ant-jars/*.jar"]
    raise "Your CAJA_SRC_PATH should point to the Caja SVN trunk." if files.empty?
    files << File.expand_path(File.join(PROTOTYPE_TEST_DIR, 'lib', 'caja', 'whitelist'))
    files << File.expand_path(File.join(self.src_path, 'ant-lib', 'com', 'google', 'caja', 'lang', 'html'))
    files.join(':')
  end
  
  def self.src_path
    raise "You must define CAJA_SRC_PATH to run cajoled tests." unless ENV['CAJA_SRC_PATH']
    File.expand_path(ENV['CAJA_SRC_PATH'])
  end
  
  
  class CompileError < StandardError
    ERRORS = %w[FATAL_ERROR ERROR Exception]
    
    def initialize(log)
      @log = log
      super(select(ERRORS).join)
    end
    
    def select(type)
      if type.is_a?(Array)
        messages.select { |m| type.find { |t| m.include?(t) }}
      else
        messages.select { |m| m.include?(type) }
      end
    end
    
    private
      def messages
        content = []
        buffer = nil
        File.open(@log).each do |l|
          if l =~ /^\S/
            content << buffer if buffer
            buffer = l
          else
            buffer << l
          end
        end
        content << buffer
      end
  end
  
  class Builder < PageBuilder
    TEMPLATES_DIR = File.join(UNITTEST_DIR, 'lib', 'caja', 'templates')
  end
  
  class GadgetBuilder < Builder
    def initialize(filename)
      super(filename, 'gadget.erb')
    end
    
    def destination
      name_file(:ext => 'html', :suffix => 'gadget')
    end
    
    def cajole
      render
      Caja.cajole({
        :i                  => destination,
        :o                  => name_file(:ext => 'js',  :suffix => 'cajoled_gadget'),
        :a                  => true,
        :log                => name_file(:ext => 'txt', :suffix => 'caja_log'),
 	:minify             => true,
#        :r                  => 'sidebyside',
        :html_attrib_schema => "resource:///html_attrib.json"
      })
    end
  end
  
  class ContainerBuilder < Builder
    def initialize(filename)
      super(filename, 'container.erb')
    end
    
    def destination
      name_file(:ext => 'html')
    end
    
    def render
      @title          = @basename.gsub('_', ' ').strip.capitalize
      @cajoled_gadget = File.basename(@filename, '.js') << '_cajoled_gadget.js'
      File.open(destination, 'w+') do |file|
        file << ERB.new(IO.read(@template), nil, '%').result(binding)
      end
    end
  end
  
  HEAP = 512
  
  def self.cajole(options = {})
    minify = options.delete(:minify)
    java("com.google.caja.plugin.PluginCompilerMain", options)
    Caja.minify(options[:o]) if minify
  end
  
  def self.minify(file)
    lines = []
    File.open(file).each { |line| lines << line.strip }
    File.open(file, 'w+') { |f| f << lines.join("\n") }
  end

  def self.java(class_name, options = {})
    log  = options.delete(:log)
    cmd  = "java #{heap} -cp #{class_path} #{class_name}#{to_option_string(options)} > #{log} 2>&1"
    raise CompileError.new(log) unless system(cmd)
  end
  
  private
    def self.to_option_string(options)
      options.reject{ |k, v| v.nil? }.map do |k, v|
         v === true ? " -#{k}" : " -#{k} #{v}"
      end.join
    end
    
    def self.heap
      (defined? HEAP) ? "-Xmx#{HEAP}m" : ''
    end
end

