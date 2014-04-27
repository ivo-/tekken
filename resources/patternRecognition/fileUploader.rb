require 'sinatra'
require 'haml'
require 'json'

get "/upload" do
  haml :upload
end

post "/upload" do
  Process.fork do
    exec("./recognizer/recognizer #{params['testfile'][:tempfile].path} #{params['count_test']} 4 5 ./results.txt")
    # exec("cat #{params['testfile'][:filename]}")
  end
  Process.wait()
  return ResultParser.perform()
end

class ResultParser
  def self.perform
    answer_list = {}
    File.open('./results.txt') do |results|
      answer_list[:counts] = results.gets.chop
      answer_list[:fn] = results.gets.chop
      while line = results.gets
        question = line.match(/.*-/).to_s.chop
        answer = line.match(/-./).to_s[1]
        answer_list[question] = answer
      end
    end
    JSON.generate(answer_list)
  end
end

