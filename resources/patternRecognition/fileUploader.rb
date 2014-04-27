require 'sinatra'
require 'haml'
require 'json'

set :protection, :except => :frame_options

get "/" do
  erb :index
end

get "/upload" do
  @count_test = params['count_test']
  haml :upload
end

post "/upload" do
  Process.fork do
    puts Dir.pwd
    exec("./recognizer/recognizer #{params['testfile'][:tempfile].path} #{params['count_test']} #{params['count_answers']} #{params['count_number']} ./results.txt")
    # exec("cat #{params['testfile'][:filename]}")
  end
  Process.wait()
  return (to_render = ResultParser.perform())
  puts to_render
  erb to_render
end

class ResultParser
  def self.perform
    json_hash = {}
    answer_list = []
    if File.exists?('./results.txt')
      File.open('./results.txt') do |results|
        json_hash[:variant] = results.gets.chop
        json_hash[:id] = results.gets.chop
        while line = results.gets
          answer = line.match(/-./).to_s[1]
          answer_list << ((answer.ord - 'a'.ord) + 1)
        end
        json_hash[:answers] = answer_list
        jso = JSON.generate(json_hash)
        puts jso
        "<script>
          window.imageData = #{jso};
        </script>"
      end
    else
      "<script>
        window.imageData = false;
      </script>"
    end
  end
end
