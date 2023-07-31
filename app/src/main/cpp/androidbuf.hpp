//
// Created by flo on 31/07/2023.
//

#ifndef POBO_ANDROIDBUF_HPP
#define POBO_ANDROIDBUF_HPP

// From https://gist.github.com/dzhioev/6127982
#include <android/log.h>

class androidbuf: public std::streambuf {
public:
		enum { bufsize = 128 }; // ... or some other suitable buffer size
		androidbuf() { this->setp(buffer, buffer + bufsize - 1); }
private:
		int overflow(int c) {
			if (c == traits_type::eof()) {
				*this->pptr() = traits_type::to_char_type(c);
				this->sbumpc();
			}
			return this->sync()? traits_type::eof(): traits_type::not_eof(c);
		}
		int sync() {
			int rc = 0;
			if (this->pbase() != this->pptr()) {
				__android_log_print(ANDROID_LOG_INFO,
				                    "pobotag C++",
				                    "%s",
				                    std::string(this->pbase(),
				                                this->pptr() - this->pbase()).c_str());
				rc = 0;
				this->setp(buffer, buffer + bufsize - 1);
			}
			return rc;
		}
		char buffer[bufsize];
};

#endif //POBO_ANDROIDBUF_HPP
