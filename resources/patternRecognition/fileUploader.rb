require 'sinatra'
require 'haml'
require 'json'

get "/upload" do
  haml :upload
end

post "/upload" do
  Process.fork do
    exec("./recognizer/recognizer #{params['testfile'][:tempfile].path} #{params['count_test']} #{params['count_answers']} #{params['count_number']} ./results.txt")
    # exec("cat #{params['testfile'][:filename]}")
  end
  Process.wait()
  return ResultParser.perform()
end

class ResultParser
  def self.perform
    answer_list = {}
    if File.exists?('./results.txt')
      File.open('./results.txt') do |results|
        answer_list[:counts] = results.gets.chop
        answer_list[:fn] = results.gets.chop
        while line = results.gets
          question = line.match(/.*-/).to_s.chop
          answer = line.match(/-./).to_s[1]
          answer_list[question] = answer
        end

        JSON.generate(answer_list)
        "<script>
          window.postMessage('#{JSON.generate(answer_list)}')
        </script>"
      end
    else
      "<script>
        window.postMessage('false')
      </script>"
    end
  end
end
