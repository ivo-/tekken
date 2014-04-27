require 'sinatra'
require 'haml'

get "/upload" do
  haml :upload
end

post "/upload" do
  Process.fork do
    # exec("./recognizer #{params['testfile'][:filename]} count_test 4 5 ./result.txt")
    exec("cat #{params['testfile'][:filename]}")
  end
  Process.wait()
  return "The test was successfully checked!"
end

